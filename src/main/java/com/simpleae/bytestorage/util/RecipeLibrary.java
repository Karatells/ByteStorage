package com.simpleae.bytestorage.util;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * «Бібліотека рецептів»: коли в Контролері встановлено відповідне покращення, мережа знає ВСІ рецепти верстака
 * гри (включно з модовими) без запису шаблонів. Рецепти — віртуальні шаблони з id {@code lib:<id рецепту>};
 * для інгредієнтів з кількома варіантами (теги) береться той предмет, якого в мережі найбільше.
 */
public final class RecipeLibrary {

    public static final String PREFIX = "lib:";
    private static final long CACHE_TICKS = 20;

    private record Entry(String id, ItemStack output, List<Ingredient> cells, boolean decompress) {
    }

    private record Cached(long time, List<PatternData> patterns) {
    }

    /** Закріплений вибір інгредієнтів для виконуваних крафтів: не змінюється, поки шаблон використовується. */
    private record Pinned(long lastUsed, PatternData pattern) {
    }

    private static final long PIN_TICKS = 400;
    private static final Map<IGrid, Map<String, Pinned>> PINS = new WeakHashMap<>();

    private static RecipeManager cachedManager;
    private static Map<String, Entry> entries = Map.of();
    private static final Map<IGrid, Cached> PATTERN_CACHE = new WeakHashMap<>();

    private RecipeLibrary() {
    }

    public static boolean isEnabled(IGrid grid) {
        NetworkControllerBlockEntity controller = grid == null ? null : grid.getController();
        return controller != null && controller.getLevel() != null && controller.hasRecipeLibrary();
    }

    /** Раскладка рецепта по сетке 3x3 (9 ингредиентов); null — рецепт не подходит (особый или больше 3x3). */
    public static List<Ingredient> layout(CraftingRecipe recipe) {
        List<Ingredient> cells = new ArrayList<>(Collections.nCopies(9, Ingredient.EMPTY));
        if (recipe instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth();
            int h = shaped.getHeight();
            List<Ingredient> ingredients = shaped.getIngredients();
            if (w > 3 || h > 3 || ingredients.size() < w * h) {
                return null;
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    cells.set(y * 3 + x, ingredients.get(y * w + x));
                }
            }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            List<Ingredient> ingredients = shapeless.getIngredients();
            if (ingredients.size() > 9) {
                return null;
            }
            for (int i = 0; i < ingredients.size(); i++) {
                cells.set(i, ingredients.get(i));
            }
        } else {
            return null;
        }
        return cells;
    }

    private static synchronized Map<String, Entry> entries(Level level) {
        RecipeManager manager = level.getRecipeManager();
        if (manager == cachedManager) {
            return entries;
        }
        Map<String, Entry> built = new LinkedHashMap<>();
        List<Entry> raw = new ArrayList<>();
        for (CraftingRecipe recipe : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
            List<Ingredient> cells = layout(recipe);
            if (cells == null) {
                continue;
            }
            ItemStack output = recipe.getResultItem(level.registryAccess());
            if (output.isEmpty()) {
                continue;
            }
            boolean valid = true;
            for (Ingredient ingredient : cells) {
                if (!ingredient.isEmpty() && ingredient.getItems().length == 0) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                raw.add(new Entry(PREFIX + recipe.getId(), output.copy(), cells, false));
            }
        }
        Map<Item, List<Entry>> byOutput = new HashMap<>();
        for (Entry e : raw) {
            byOutput.computeIfAbsent(e.output().getItem(), k -> new ArrayList<>()).add(e);
        }
        for (Entry e : raw) {
            built.put(e.id(), new Entry(e.id(), e.output(), e.cells(), isDecompress(e, byOutput)));
        }
        cachedManager = manager;
        entries = built;
        PATTERN_CACHE.clear();
        return entries;
    }

    /**
     * «Распаковка»: рецепт с одним ингредиентом и выходом больше 1, для которого есть обратный рецепт
     * (результат -> исходный предмет), например блок -> 9 слитков.
     */
    private static boolean isDecompress(Entry e, Map<Item, List<Entry>> byOutput) {
        if (e.output().getCount() < 2) {
            return false;
        }
        Ingredient single = null;
        for (Ingredient i : e.cells()) {
            if (!i.isEmpty()) {
                if (single != null) {
                    return false;
                }
                single = i;
            }
        }
        if (single == null) {
            return false;
        }
        Item out = e.output().getItem();
        for (ItemStack option : single.getItems()) {
            for (Entry reverse : byOutput.getOrDefault(option.getItem(), List.of())) {
                boolean allOut = true;
                boolean any = false;
                for (Ingredient i : reverse.cells()) {
                    if (i.isEmpty()) {
                        continue;
                    }
                    any = true;
                    boolean has = false;
                    for (ItemStack o : i.getItems()) {
                        if (o.getItem() == out) {
                            has = true;
                            break;
                        }
                    }
                    if (!has) {
                        allOut = false;
                        break;
                    }
                }
                if (any && allOut) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Сбросить кэши (после изменения настроек рецептов в Контроллере). */
    public static synchronized void invalidate() {
        PATTERN_CACHE.clear();
        PINS.clear();
    }

    private static boolean allowed(Entry entry, NetworkControllerBlockEntity c) {
        if (c.isSkipDecompress() && entry.decompress()) {
            return false;
        }
        for (int i = 0; i < c.getRecipeBlacklist().getSlots(); i++) {
            ItemStack banned = c.getRecipeBlacklist().getStackInSlot(i);
            if (!banned.isEmpty() && banned.getItem() == entry.output().getItem()) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Integer> availability(IGrid grid) {
        Map<String, Integer> available = new HashMap<>();
        INetworkStorage storage = grid.getStorage();
        if (storage != null) {
            for (ItemStack stack : storage.getAllItems()) {
                available.merge(CraftingService.key(stack), stack.getCount(), Integer::sum);
            }
        }
        return available;
    }

    private static PatternData build(Entry entry, Map<String, Integer> available, NetworkControllerBlockEntity c) {
        List<ItemStack> grid = new ArrayList<>(9);
        for (Ingredient ingredient : entry.cells()) {
            if (ingredient.isEmpty()) {
                grid.add(ItemStack.EMPTY);
                continue;
            }
            ItemStack[] options = ingredient.getItems();
            ItemStack best = options[0];
            ItemStack preferred = preferred(options, c);
            int bestCount = available.getOrDefault(CraftingService.key(best), 0);
            for (int i = 1; preferred == null && i < options.length; i++) {
                int count = available.getOrDefault(CraftingService.key(options[i]), 0);
                if (count > bestCount) {
                    best = options[i];
                    bestCount = count;
                }
            }
            ItemStack one = (preferred != null ? preferred : best).copy();
            one.setCount(1);
            grid.add(one);
        }
        return new PatternData(entry.id(), entry.output(), grid);
    }

    private static ItemStack preferred(ItemStack[] options, NetworkControllerBlockEntity c) {
        if (options.length < 2) {
            return null;
        }
        for (int p = 0; p < c.getRecipePrefs().getSlots(); p++) {
            ItemStack pref = c.getRecipePrefs().getStackInSlot(p);
            if (pref.isEmpty()) {
                continue;
            }
            for (ItemStack option : options) {
                if (option.getItem() == pref.getItem()) {
                    return option;
                }
            }
        }
        return null;
    }

    /** Усі віртуальні шаблони бібліотеки (порожньо, якщо покращення немає). */
    public static List<PatternData> patterns(IGrid grid) {
        if (!isEnabled(grid)) {
            return List.of();
        }
        Level level = grid.getController().getLevel();
        long now = level.getGameTime();
        synchronized (RecipeLibrary.class) {
            Cached cached = PATTERN_CACHE.get(grid);
            if (cached != null && now - cached.time() < CACHE_TICKS && now >= cached.time()) {
                return cached.patterns();
            }
        }
        Map<String, Entry> all = entries(level);
        Map<String, Integer> available = availability(grid);
        List<PatternData> result = new ArrayList<>(all.size());
        NetworkControllerBlockEntity c = grid.getController();
        for (Entry entry : all.values()) {
            if (allowed(entry, c)) {
                result.add(build(entry, available, c));
            }
        }
        synchronized (RecipeLibrary.class) {
            PATTERN_CACHE.put(grid, new Cached(now, result));
        }
        return result;
    }

    /**
     * Шаблон бібліотеки за id ({@code lib:...}); null, якщо це не він або покращення немає.
     * Вибір варіантів інгредієнтів закріплюється, поки шаблон використовується (щоб Асемблер не «передумав» посеред крафту).
     */
    public static PatternData find(IGrid grid, String id) {
        if (id == null || !id.startsWith(PREFIX) || !isEnabled(grid)) {
            return null;
        }
        Level level = grid.getController().getLevel();
        long now = level.getGameTime();
        synchronized (RecipeLibrary.class) {
            Map<String, Pinned> pins = PINS.computeIfAbsent(grid, g -> new HashMap<>());
            Pinned pinned = pins.get(id);
            if (pinned != null && now >= pinned.lastUsed() && now - pinned.lastUsed() <= PIN_TICKS) {
                pins.put(id, new Pinned(now, pinned.pattern()));
                return pinned.pattern();
            }
            Entry entry = entries(level).get(id);
            if (entry == null || !allowed(entry, grid.getController())) {
                return null;
            }
            PatternData built = build(entry, availability(grid), grid.getController());
            pins.put(id, new Pinned(now, built));
            return built;
        }
    }

    public static int count(IGrid grid) {
        if (!isEnabled(grid)) {
            return 0;
        }
        NetworkControllerBlockEntity c = grid.getController();
        int n = 0;
        for (Entry e : entries(c.getLevel()).values()) {
            if (allowed(e, c)) {
                n++;
            }
        }
        return n;
    }
}
