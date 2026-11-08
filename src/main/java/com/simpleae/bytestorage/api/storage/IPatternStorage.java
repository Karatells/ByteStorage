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
        private final boolean processing;
        private final List<ItemStack> extras;

        public PatternData(String id, ItemStack output, List<ItemStack> grid) {
            this(id, output, grid, false);
        }

        /**
         * @param processing true — «процесний» шаблон для машини (піч, дробарка...): у клітинках лежать
         *                   предмети з кількостями, а не розкладка верстака.
         */
        public PatternData(String id, ItemStack output, List<ItemStack> grid, boolean processing) {
            this(id, output, grid, processing, List.of());
        }

        /**
         * @param extras побочные продукты на один крафт (процесные шаблоны): шина забирает их из машины вместе
         *               с основным результатом, чтобы они не забивали машину. Планировщик их не учитывает.
         */
        public PatternData(String id, ItemStack output, List<ItemStack> grid, boolean processing, List<ItemStack> extras) {
            this.id = id;
            List<ItemStack> extraCopy = new ArrayList<>();
            for (ItemStack extra : extras) {
                if (!extra.isEmpty()) {
                    extraCopy.add(extra.copy());
                }
            }
            this.extras = extraCopy;
            this.processing = processing;
            this.output = output.copy();
            this.grid = new ArrayList<>(GRID_SIZE);
            for (int i = 0; i < GRID_SIZE; i++) {
                ItemStack cell = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
                if (cell.isEmpty()) {
                    this.grid.add(ItemStack.EMPTY);
                } else {
                    ItemStack one = cell.copy();
                    if (!processing) {
                        one.setCount(1);
                    }
                    this.grid.add(one);
                }
            }
            this.inputs = aggregate(this.grid);
        }

        public PatternData(ItemStack output, List<ItemStack> grid) {
            this(makeId(output, grid, false), output, grid, false);
        }

        public static PatternData processing(ItemStack output, List<ItemStack> inputs) {
            return new PatternData(makeId(output, inputs, true), output, inputs, true);
        }

        public static PatternData processing(ItemStack output, List<ItemStack> inputs, List<ItemStack> extras) {
            String id = makeId(output, inputs, true);
            if (!extras.isEmpty()) {
                StringBuilder sb = new StringBuilder(id);
                for (ItemStack extra : extras) {
                    sb.append('+').append(extra.getCount()).append(stackKey(extra));
                }
                id = id + "_" + String.format("%08x", sb.toString().hashCode());
            }
            return new PatternData(id, output, inputs, true, extras);
        }

        /** Побочные продукты одного крафта (может быть пусто). */
        public List<ItemStack> getExtras() {
            return extras;
        }

        /** Чи це шаблон для машини (кладеться в Шину Шаблонів), а не крафт верстака. */
        public boolean isProcessing() {
            return processing;
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
            tag.putBoolean("Processing", processing);
            return tag;
        }

        public static PatternData load(CompoundTag tag) {
            ItemStack output = ItemStack.of(tag.getCompound("Output"));
            ListTag list = tag.getList("Grid", 10);
            List<ItemStack> grid = new ArrayList<>(GRID_SIZE);
            for (int i = 0; i < list.size() && i < GRID_SIZE; i++) {
                grid.add(ItemStack.of(list.getCompound(i)));
            }
            boolean processing = tag.getBoolean("Processing");
            String id = tag.getString("Id");
            if (id.isEmpty()) {
                id = makeId(output, grid, processing);
            }
            return new PatternData(id, output, grid, processing);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(id, 128);
            buf.writeItem(output);
            for (ItemStack cell : grid) {
                buf.writeItem(cell);
            }
            buf.writeBoolean(processing);
        }

        public static PatternData read(FriendlyByteBuf buf) {
            String id = buf.readUtf(128);
            ItemStack output = buf.readItem();
            List<ItemStack> grid = new ArrayList<>(GRID_SIZE);
            for (int i = 0; i < GRID_SIZE; i++) {
                grid.add(buf.readItem());
            }
            boolean processing = buf.readBoolean();
            return new PatternData(id, output, grid, processing);
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
            return makeId(output, grid, false);
        }

        public static String makeId(ItemStack output, List<ItemStack> grid, boolean processing) {
            StringBuilder sb = new StringBuilder();
            sb.append(processing ? "P:" : "C:");
            sb.append(output.getCount()).append('x');
            sb.append(stackKey(output)).append('>');
            for (int i = 0; i < GRID_SIZE; i++) {
                ItemStack cell = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
                sb.append(cell.isEmpty() ? "-" : (processing ? cell.getCount() + "x" : "") + stackKey(cell)).append(';');
            }
            net.minecraft.resources.ResourceLocation key =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(output.getItem());
            String path = key != null ? key.getPath() : "unknown";
            return (processing ? "proc_" : "") + path + "_" + String.format("%08x", sb.toString().hashCode());
        }

        private static String stackKey(ItemStack stack) {
            net.minecraft.resources.ResourceLocation key =
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            String name = key != null ? key.toString() : "unknown";
            return stack.hasTag() ? name + stack.getTag() : name;
        }
    }
}
