package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.network.packet.CraftPlanPacket;
import com.simpleae.bytestorage.network.packet.CraftRequestPacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.util.CraftingService;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Общие возможности экранов терминалов: сортировка списка предметов (имя/количество, по возрастанию/убыванию)
 * и заказ автокрафта из любого терминала (Ctrl+клик или клик по предмету «0 в наличии»).
 */
public class TerminalExtras {

    // Настройки сортировки общие для всех терминалов и сохраняются на время игры
    private static boolean byCount = false;
    private static boolean descending = false;

    private static List<ItemStack> craftables = new ArrayList<>();
    private static Set<String> craftableKeys = new HashSet<>();

    public static void setCraftables(List<ItemStack> items) {
        craftables = items;
        Set<String> keys = new HashSet<>();
        for (ItemStack s : items) {
            keys.add(CraftingService.key(s));
        }
        craftableKeys = keys;
    }

    private static final NumberFormat FMT = NumberFormat.getInstance(Locale.US);
    private static final int SORT_W = 40;
    private static final int SORT_H = 14;

    private Set<String> poolKeys = new HashSet<>();

    private ItemStack dialogStack = ItemStack.EMPTY;
    private int dialogAmount = 1;

    // --- Список ---

    private record Row(ItemStack stack, String name, int count) {
    }

    /** Предмет сети + предметы, доступные только для крафта (количество 0); фильтр по тексту и сортировка. */
    public List<ItemStack> view(List<ItemStack> pool, String query) {
        Set<String> keys = new HashSet<>();
        List<Row> rows = new ArrayList<>();
        for (ItemStack s : pool) {
            keys.add(CraftingService.key(s));
        }
        for (ItemStack s : pool) {
            rows.add(new Row(s, s.getHoverName().getString(), s.getCount()));
        }
        for (ItemStack s : craftables) {
            if (!keys.contains(CraftingService.key(s))) {
                rows.add(new Row(s, s.getHoverName().getString(), 0));
            }
        }
        this.poolKeys = keys;

        List<String> tokens = new ArrayList<>();
        if (query != null) {
            for (String t : query.trim().toLowerCase().split("\\s+")) {
                if (!t.isEmpty()) {
                    tokens.add(t);
                }
            }
        }
        if (!tokens.isEmpty()) {
            rows.removeIf(r -> !matches(r, tokens, keys));
        }
        // Сначала предметы, которые есть в сети, потом — те, что можно только скрафтить (внутри групп — выбранная сортировка)
        rows.sort((a, b) -> {
            boolean ga = a.count() == 0 && !keys.contains(CraftingService.key(a.stack()));
            boolean gb = b.count() == 0 && !keys.contains(CraftingService.key(b.stack()));
            if (ga != gb) {
                return ga ? 1 : -1;
            }
            boolean fa = isFavorite(a.stack());
            boolean fb = isFavorite(b.stack());
            if (fa != fb) {
                return fa ? -1 : 1;
            }
            int c = byCount && !ga ? Integer.compare(a.count(), b.count()) : a.name().compareToIgnoreCase(b.name());
            if (c == 0) {
                c = byCount && !ga ? a.name().compareToIgnoreCase(b.name()) : Integer.compare(a.count(), b.count());
            }
            return descending ? -c : c;
        });
        List<ItemStack> result = new ArrayList<>(rows.size());
        for (Row r : rows) {
            result.add(r.stack());
        }
        return result;
    }

    /**
     * Расширенный поиск: слова через пробел (все должны подойти).
     * {@code @мод} — id мода, {@code #тег} — тег предмета, {@code >N} {@code <N} {@code =N} — количество,
     * {@code $craftable} — можно скрафтить, {@code $stock} — есть в сети, {@code $fav} — избранное; иначе — часть названия.
     */
    private boolean matches(Row r, List<String> tokens, Set<String> inStock) {
        ItemStack stack = r.stack();
        for (String t : tokens) {
            boolean ok;
            if (t.startsWith("@") && t.length() > 1) {
                net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                ok = id != null && id.getNamespace().contains(t.substring(1));
            } else if (t.startsWith("#") && t.length() > 1) {
                String tag = t.substring(1);
                ok = stack.getTags().anyMatch(k -> k.location().toString().contains(tag));
            } else if ((t.startsWith(">") || t.startsWith("<") || t.startsWith("=")) && t.length() > 1) {
                int n;
                try {
                    n = Integer.parseInt(t.substring(1));
                    int c = r.count();
                    ok = t.charAt(0) == '>' ? c > n : (t.charAt(0) == '<' ? c < n : c == n);
                } catch (NumberFormatException e) {
                    ok = false;
                }
            } else if (t.startsWith("$")) {
                String k = t.substring(1);
                if ("craftable".startsWith(k) && !k.isEmpty()) {
                    ok = isCraftable(stack);
                } else if ("stock".startsWith(k) && !k.isEmpty()) {
                    ok = inStock.contains(CraftingService.key(stack));
                } else if ("fav".startsWith(k) && !k.isEmpty()) {
                    ok = isFavorite(stack);
                } else {
                    ok = false;
                }
            } else {
                ok = r.name().toLowerCase().contains(t);
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    // --- Избранное (хранится локально в config) ---

    private static Set<String> favorites;

    private static java.nio.file.Path favoritesFile() {
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("simple_bytestorage_favorites.txt");
    }

    private static synchronized Set<String> favorites() {
        if (favorites == null) {
            favorites = new HashSet<>();
            try {
                java.nio.file.Path p = favoritesFile();
                if (java.nio.file.Files.exists(p)) {
                    favorites.addAll(java.nio.file.Files.readAllLines(p));
                }
            } catch (Exception ignored) {
                // файл не обязателен
            }
        }
        return favorites;
    }

    public static boolean isFavorite(ItemStack stack) {
        return favorites().contains(CraftingService.key(stack));
    }

    public static void toggleFavorite(ItemStack stack) {
        Set<String> f = favorites();
        String key = CraftingService.key(stack);
        if (!f.remove(key)) {
            f.add(key);
        }
        try {
            java.nio.file.Files.write(favoritesFile(), new ArrayList<>(f));
        } catch (Exception ignored) {
            // не удалось сохранить — останется до конца игры
        }
    }

    /** Золотая отметка избранного в углу ячейки. */
    public static void drawFavorite(GuiGraphics g, int itemX, int itemY) {
        g.fill(itemX, itemY, itemX + 4, itemY + 4, 0xFF1B1E24);
        g.fill(itemX + 1, itemY + 1, itemX + 3, itemY + 3, 0xFFFFD34D);
    }

    /** Предмет есть только как рецепт (в сети его нет). */
    public boolean isGhost(ItemStack stack) {
        return !poolKeys.contains(CraftingService.key(stack));
    }

    /** Подпись вместо количества для предметов, которых нет в сети, но которые можно скрафтить. */
    public static String craftLabel() {
        return Component.translatable("gui.simple_bytestorage.terminal.craft_label").getString();
    }

    public boolean isCraftable(ItemStack stack) {
        return craftableKeys.contains(CraftingService.key(stack));
    }

    // --- Сортировка (кнопки справа от окна) ---

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String keyLabel() {
        return Component.translatable(byCount ? "gui.simple_bytestorage.sort.count" : "gui.simple_bytestorage.sort.name").getString();
    }

    private String dirLabel() {
        String k = byCount ? (descending ? "91" : "19") : (descending ? "za" : "az");
        return Component.translatable("gui.simple_bytestorage.sort." + k).getString();
    }

    public void renderSort(GuiGraphics g, Font font, int leftPos, int topPos, int imageWidth, int mx, int my) {
        int x = leftPos + imageWidth + 2;
        UiButton.paint(g, font, x, topPos + 4, SORT_W, SORT_H, keyLabel(), inside(mx, my, x, topPos + 4, SORT_W, SORT_H), true);
        UiButton.paint(g, font, x, topPos + 4 + SORT_H + 2, SORT_W, SORT_H, dirLabel(),
                inside(mx, my, x, topPos + 4 + SORT_H + 2, SORT_W, SORT_H), true);
        if (inside(mx, my, x, topPos + 4, SORT_W, SORT_H)) {
            g.renderTooltip(font, Component.translatable("gui.simple_bytestorage.sort.key.tip"), mx, my);
        } else if (inside(mx, my, x, topPos + 4 + SORT_H + 2, SORT_W, SORT_H)) {
            g.renderTooltip(font, Component.translatable("gui.simple_bytestorage.sort.dir.tip"), mx, my);
        }
    }

    /** @return true, если клик пришёлся на кнопку сортировки. */
    public boolean clickSort(double mx, double my, int button, int leftPos, int topPos, int imageWidth) {
        if (button != 0) {
            return false;
        }
        int x = leftPos + imageWidth + 2;
        if (inside(mx, my, x, topPos + 4, SORT_W, SORT_H)) {
            byCount = !byCount;
            return true;
        }
        if (inside(mx, my, x, topPos + 4 + SORT_H + 2, SORT_W, SORT_H)) {
            descending = !descending;
            return true;
        }
        return false;
    }

    // --- Диалог заказа (тот же вид, что окно подтверждения в Терминале шаблонов) ---

    private static final int EXTRA = 22;
    private static final int[] STEPS = {-64, -10, -1, 1, 10, 64};

    private static List<CraftPlanPacket.Entry> planSteps = new ArrayList<>();
    private static List<CraftPlanPacket.Entry> planMissing = new ArrayList<>();
    private static boolean planReady = false;

    /** Ответ сервера с планом (вызывается пакетом). */
    public static void setPlan(String patternId, int crafts, List<CraftPlanPacket.Entry> steps,
                               List<CraftPlanPacket.Entry> missing) {
        planSteps = steps;
        planMissing = missing;
        planReady = true;
    }

    public boolean isDialogOpen() {
        return !dialogStack.isEmpty();
    }

    public void openDialog(ItemStack stack) {
        ItemStack one = stack.copy();
        one.setCount(1);
        this.dialogStack = one;
        this.dialogAmount = 1;
        requestPlan();
    }

    public void closeDialog() {
        this.dialogStack = ItemStack.EMPTY;
        planReady = false;
    }

    private void requestPlan() {
        planReady = false;
        planSteps = new ArrayList<>();
        planMissing = new ArrayList<>();
        SimpleByteStoragePackets.sendToServer(new CraftRequestPacket(CraftRequestPacket.ACTION_PLAN, dialogStack, dialogAmount));
    }

    private boolean planOk() {
        return planReady && planMissing.isEmpty() && !planSteps.isEmpty();
    }

    private int dialogX(int screenW) {
        return (screenW - CraftPlanView.W) / 2;
    }

    private int dialogY(int screenH) {
        return (screenH - (CraftPlanView.BASE_H + EXTRA)) / 2;
    }

    public void renderDialog(GuiGraphics g, Font font, int screenW, int screenH, int mx, int my) {
        if (!isDialogOpen()) {
            return;
        }
        int x = dialogX(screenW);
        int y = dialogY(screenH);
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 400.0F);
        g.fill(0, 0, screenW, screenH, 0x90000000);
        CraftPlanView.draw(g, font, x, y, EXTRA,
                Component.translatable("gui.simple_bytestorage.order.title", dialogStack.getHoverName()),
                planSteps, planMissing, planOk(), mx, my);
        g.renderItem(dialogStack, x + CraftPlanView.W - 22, y + 2);
        for (int i = 0; i < 3; i++) {
            drawStep(g, font, x + 6 + i * 26, y + 17, STEPS[i], mx, my);
        }
        String amount = FMT.format(dialogAmount);
        g.drawString(font, amount, x + CraftPlanView.W / 2 - font.width(amount) / 2, y + 21, GuiStyle.ACCENT, false);
        for (int i = 3; i < 6; i++) {
            drawStep(g, font, x + CraftPlanView.W - 6 - 26 * (6 - i) + 2, y + 17, STEPS[i], mx, my);
        }
        if (!planReady) {
            String wait = Component.translatable("gui.simple_bytestorage.order.calculating").getString();
            g.drawString(font, wait, x + 10, y + 50, GuiStyle.TEXT_MUTED, false);
        }
        g.pose().popPose();
    }

    private void drawStep(GuiGraphics g, Font font, int x, int y, int step, int mx, int my) {
        UiButton.paint(g, font, x, y, 24, 14, (step > 0 ? "+" : "") + step, inside(mx, my, x, y, 24, 14), true);
    }

    /** Модальный диалог: поглощает все клики, пока открыт. */
    public boolean clickDialog(double mx, double my, int button, int screenW, int screenH) {
        if (!isDialogOpen()) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        int x = dialogX(screenW);
        int y = dialogY(screenH);
        for (int i = 0; i < 6; i++) {
            int bx = i < 3 ? x + 6 + i * 26 : x + CraftPlanView.W - 6 - 26 * (6 - i) + 2;
            if (inside(mx, my, bx, y + 17, 24, 14)) {
                int next = Math.max(1, Math.min(CraftRequestPacket.MAX_AMOUNT, dialogAmount + STEPS[i]));
                if (next != dialogAmount) {
                    dialogAmount = next;
                    requestPlan();
                }
                return true;
            }
        }
        if (CraftPlanView.startHit(mx, my, x, y, EXTRA) && planOk()) {
            SimpleByteStoragePackets.sendToServer(new CraftRequestPacket(CraftRequestPacket.ACTION_ORDER, dialogStack, dialogAmount));
            closeDialog();
        } else if (CraftPlanView.cancelHit(mx, my, x, y, EXTRA)) {
            closeDialog();
        }
        return true;
    }
}
