package com.simpleae.bytestorage.api.storage;

import com.simpleae.bytestorage.api.network.IGrid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Глобальне сховище шаблонів крафту мережі.
 *
 * Шаблон — це рецепт верстака 3×3: розташування інгредієнтів у сітці + результат.
 * Усі шаблони живуть на рівні мережі (зберігаються в NBT контролера), тому їх бачать
 * Термінал Шаблонів та всі Асемблери мережі.
 */
public interface IPatternStorage {

    /**
     * Додати шаблон у мережу. Ідентичні рецепти (той самий результат і та сама сітка)
     * не дублюються — повертається вже наявний запис.
     *
     * @param output результат одного крафту
     * @param grid   9 елементів (3×3, ряд за рядом); порожні клітинки — {@link ItemStack#EMPTY}
     */
    PatternData addPattern(ItemStack output, List<ItemStack> grid);

    boolean removePattern(String id);

    @Nullable
    PatternData getPattern(String id);

    Collection<PatternData> getAllPatterns();

    /**
     * Чи є зараз у віртуальному пулі мережі всі інгредієнти шаблону.
     */
    boolean canCraft(IGrid grid, String id);

    int size();

    /**
     * Лічильник змін; зростає при кожному додаванні/видаленні шаблону.
     */
    int getVersion();

    ListTag save();

    void load(ListTag tag);

    /**
     * Дані шаблону крафту (незмінні).
     */
    class PatternData {
        public static final int GRID_SIZE = 9;

        private final String id;
        private final ItemStack output;
        private final List<ItemStack> grid;
        private final List<ItemStack> inputs;

        public PatternData(String id, ItemStack output, List<ItemStack> grid) {
            this.id = id;
            this.output = output.copy();
            this.grid = new ArrayList<>(GRID_SIZE);
            for (int i = 0; i < GRID_SIZE; i++) {
                ItemStack cell = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
                if (cell.isEmpty()) {
                    this.grid.add(ItemStack.EMPTY);
                } else {
                    ItemStack one = cell.copy();
                    one.setCount(1);
                    this.grid.add(one);
                }
            }
            this.inputs = aggregate(this.grid);
        }

        public PatternData(ItemStack output, List<ItemStack> grid) {
            this(makeId(output, grid), output, grid);
        }

        /** Унікальний ключ шаблону (стабільний для того самого рецепту). */
        public String getId() {
            return id;
        }

        /** Сумісність зі старим API. */
        public String getName() {
            return id;
        }

        public ItemStack getOutput() {
            return output;
        }

        /** Сітка 3×3 (9 елементів, порожні клітинки — EMPTY). */
        public List<ItemStack> getGrid() {
            return grid;
        }

        /** Зведений список інгредієнтів: однакові предмети складені, count = скільки потрібно на 1 крафт. */
        public List<ItemStack> getInputs() {
            return inputs;
        }

        public int getTotalInputCount() {
            int total = 0;
            for (ItemStack stack : inputs) {
                total += stack.getCount();
            }
            return total;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", id);
            tag.put("Output", output.save(new CompoundTag()));
            ListTag list = new ListTag();
            for (ItemStack cell : grid) {
                list.add(cell.save(new CompoundTag()));
            }
            tag.put("Grid", list);
            return tag;
        }

        public static PatternData load(CompoundTag tag) {
            ItemStack output = ItemStack.of(tag.getCompound("Output"));
            ListTag list = tag.getList("Grid", 10);
            List<ItemStack> grid = new ArrayList<>(GRID_SIZE);
            for (int i = 0; i < list.size() && i < GRID_SIZE; i++) {
                grid.add(ItemStack.of(list.getCompound(i)));
            }
            String id = tag.getString("Id");
            if (id.isEmpty()) {
                id = makeId(output, grid);
            }
            return new PatternData(id, output, grid);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(id, 128);
            buf.writeItem(output);
            for (ItemStack cell : grid) {
                buf.writeItem(cell);
            }
        }

        public static PatternData read(FriendlyByteBuf buf) {
            String id = buf.readUtf(128);
            ItemStack output = buf.readItem();
            List<ItemStack> grid = new ArrayList<>(GRID_SIZE);
            for (int i = 0; i < GRID_SIZE; i++) {
                grid.add(buf.readItem());
            }
            return new PatternData(id, output, grid);
        }

        /** Склеює однакові предмети (з урахуванням NBT) у список «предмет × кількість». */
        public static List<ItemStack> aggregate(List<ItemStack> cells) {
            List<ItemStack> result = new ArrayList<>();
            for (ItemStack cell : cells) {
                if (cell.isEmpty()) {
                    continue;
                }
                boolean merged = false;
                for (ItemStack existing : result) {
                    if (ItemStack.isSameItemSameTags(existing, cell)) {
                        existing.grow(cell.getCount());
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    result.add(cell.copy());
                }
            }
            return result;
        }

        /** Ключ: ім'я предмета-результату + 8 hex-цифр хеша всього рецепту. */
        public static String makeId(ItemStack output, List<ItemStack> grid) {
            StringBuilder sb = new StringBuilder();
            sb.append(stackKey(output)).append('>');
            for (int i = 0; i < GRID_SIZE; i++) {
                ItemStack cell = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
                sb.append(cell.isEmpty() ? "-" : stackKey(cell)).append(';');
            }
            net.minecraft.resources.ResourceLocation key =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(output.getItem());
            String path = key != null ? key.getPath() : "unknown";
            return path + "_" + String.format("%08x", sb.toString().hashCode());
        }

        private static String stackKey(ItemStack stack) {
            net.minecraft.resources.ResourceLocation key =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            String name = key != null ? key.toString() : "unknown";
            return stack.hasTag() ? name + stack.getTag() : name;
        }
    }
}
