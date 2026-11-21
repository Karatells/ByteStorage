package com.simpleae.bytestorage.storage;

import com.simpleae.bytestorage.api.storage.CellTier;
import com.simpleae.bytestorage.api.storage.ICellInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Реалізація інвентаря для диска зберігання на основі NBT.
 * 
 * Бізнес-правила Simple Byte Storage:
 * 1. Відсутність ліміту на типи (No Item Types limit) — список необмежений кількістю унікальних предметів.
 * 2. 1 предмет = 1 байт.
 * 3. Використані байти = сума кількостей усіх збережених предметів.
 */
public class ByteCellInventory implements ICellInventory {

    public static final String TAG_INVENTORY = "ByteInventory";
    public static final String TAG_USED_BYTES = "UsedBytes";
    public static final String TAG_BYTE_COUNT = "ByteCount";
    public static final String TAG_CELL_FILTER = "CellFilter";
    public static final String TAG_CELL_BLACKLIST = "CellBlacklist";

    private final ItemStack cellStack;
    private final CellTier tier;
    private final Map<ItemKey, ItemStack> itemMap = new LinkedHashMap<>();
    private final List<ItemStack> cellFilter = new ArrayList<>();
    private boolean cellBlacklist = false;
    private final boolean autoFlush;
    private List<ItemStack> cachedStoredItems;
    private int cachedUsedBytes;
    private boolean dirty;

    public ByteCellInventory(ItemStack cellStack, CellTier tier) {
        this(cellStack, tier, true);
    }

    public ByteCellInventory(ItemStack cellStack, CellTier tier, boolean autoFlush) {
        this.cellStack = cellStack;
        this.tier = tier;
        this.autoFlush = autoFlush;
        this.loadFromTag();
    }

    /**
     * Завантажує список предметів з NBT диска.
     * Зауважте: використовується поле "ByteCount" типу int, оскільки
     * ванільний ItemStack.save() записує Count як байт (макс. 127),
     * а нам потрібна підтримка тисяч предметів в одній позиції.
     */
    private void loadFromTag() {
        itemMap.clear();
        cachedStoredItems = null;
        cachedUsedBytes = 0;
        dirty = false;

        CompoundTag rootTag = cellStack.getTag();
        cellFilter.clear();
        cellBlacklist = false;
        if (rootTag != null) {
            cellBlacklist = rootTag.getBoolean(TAG_CELL_BLACKLIST);
            if (rootTag.contains(TAG_CELL_FILTER, Tag.TAG_LIST)) {
                ListTag fList = rootTag.getList(TAG_CELL_FILTER, Tag.TAG_COMPOUND);
                for (int i = 0; i < fList.size(); i++) {
                    ItemStack fs = ItemStack.of(fList.getCompound(i));
                    if (!fs.isEmpty()) {
                        cellFilter.add(fs);
                    }
                }
            }
        }
        if (rootTag == null || !rootTag.contains(TAG_INVENTORY, Tag.TAG_LIST)) {
            return;
        }

        ListTag listTag = rootTag.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag entryTag = listTag.getCompound(i);
            // Count у ванільному NBT — байт (переповнюється вже на 128). Реальну кількість беремо з ByteCount,
            // тому для читання підставляємо Count=1, щоб запис не вважався порожнім.
            CompoundTag readTag = entryTag.copy();
            if (entryTag.contains(TAG_BYTE_COUNT, Tag.TAG_INT)) {
                readTag.putByte("Count", (byte) 1);
            }
            ItemStack stack = ItemStack.of(readTag);

            if (!stack.isEmpty()) {
                if (entryTag.contains(TAG_BYTE_COUNT, Tag.TAG_INT)) {
                    stack.setCount(entryTag.getInt(TAG_BYTE_COUNT));
                }
                ItemKey key = ItemKey.of(stack);
                ItemStack existing = itemMap.get(key);
                if (existing == null) {
                    itemMap.put(key, stack);
                } else {
                    existing.grow(stack.getCount());
                }
                cachedUsedBytes += stack.getCount();
            }
        }
    }

    /**
     * Зберігає поточний стан інвентаря назад у NBT диска.
     */
    private void saveToTag() {
        CompoundTag rootTag = cellStack.getOrCreateTag();
        ListTag listTag = new ListTag();

        int totalBytes = 0;
        for (ItemStack stack : itemMap.values()) {
            if (!stack.isEmpty()) {
                CompoundTag entryTag = stack.save(new CompoundTag());
                entryTag.putByte("Count", (byte) 1); // справжня кількість — у ByteCount (див. loadFromTag)
                entryTag.putInt(TAG_BYTE_COUNT, stack.getCount());
                listTag.add(entryTag);
                totalBytes += stack.getCount();
            }
        }

        rootTag.put(TAG_INVENTORY, listTag);
        rootTag.putInt(TAG_USED_BYTES, totalBytes);
        this.cachedUsedBytes = totalBytes;
        this.dirty = false;
    }

    private void markDirty() {
        cachedStoredItems = null;
        if (autoFlush) {
            saveToTag();
        } else {
            dirty = true;
        }
    }

    @Override
    public void flush() {
        if (dirty) {
            saveToTag();
        }
    }

    @Override
    public int getMaxBytes() {
        return tier.getCapacityBytes();
    }

    @Override
    public int getUsedBytes() {
        return cachedUsedBytes;
    }

    @Override
    public List<ItemStack> getStoredItems() {
        if (cachedStoredItems == null) {
            cachedStoredItems = Collections.unmodifiableList(new ArrayList<>(itemMap.values()));
        }
        return cachedStoredItems;
    }

    @Override
    public ItemStack insertItem(ItemStack toInsert, boolean simulate) {
        if (toInsert.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int freeBytes = getFreeBytes();
        if (freeBytes <= 0) {
            return toInsert.copy();
        }

        // Кількість предметів, які ми реально можемо прийняти в межах ліміту байтів
        int insertCount = Math.min(toInsert.getCount(), freeBytes);
        if (insertCount <= 0) {
            return toInsert.copy();
        }

        if (!simulate) {
            ItemKey key = ItemKey.of(toInsert);
            ItemStack existing = itemMap.get(key);
            if (existing != null) {
                existing.grow(insertCount);
            } else {
                // Якщо такого предмета ще немає, додаємо новий запис (немає ліміту на унікальні типи!)
                ItemStack newEntry = toInsert.copy();
                newEntry.setCount(insertCount);
                itemMap.put(ItemKey.owned(newEntry), newEntry);
            }

            cachedUsedBytes += insertCount;
            markDirty();
        }

        // Залишок, який не помістився
        int remainderCount = toInsert.getCount() - insertCount;
        if (remainderCount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = toInsert.copy();
        remainder.setCount(remainderCount);
        return remainder;
    }

    @Override
    public ItemStack extractItem(ItemStack toExtract, int amount, boolean simulate) {
        if (toExtract.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemKey key = ItemKey.of(toExtract);
        ItemStack existing = itemMap.get(key);
        if (existing == null || existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int extractCount = Math.min(existing.getCount(), amount);
        ItemStack extractedStack = existing.copy();
        extractedStack.setCount(extractCount);

        if (!simulate) {
            existing.shrink(extractCount);
            if (existing.isEmpty()) {
                itemMap.remove(key);
            }
            cachedUsedBytes = Math.max(0, cachedUsedBytes - extractCount);
            markDirty();
        }

        return extractedStack;
    }

    @Override
    public boolean acceptsItem(ItemStack stack) {
        if (cellFilter.isEmpty()) {
            return true;
        }
        boolean matched = false;
        for (ItemStack f : cellFilter) {
            if (ItemStack.isSameItemSameTags(f, stack)) {
                matched = true;
                break;
            }
        }
        return cellBlacklist != matched;
    }

    @Override
    public boolean containsItem(ItemStack match) {
        if (match.isEmpty()) {
            return false;
        }
        ItemStack existing = itemMap.get(ItemKey.of(match));
        return existing != null && !existing.isEmpty();
    }

    @Override
    public void clear() {
        itemMap.clear();
        cachedUsedBytes = 0;
        markDirty();
    }

    private static final class ItemKey {
        private final Item item;
        private final CompoundTag tag;
        private final int hash;

        private ItemKey(Item item, CompoundTag tag) {
            this.item = item;
            this.tag = tag;
            this.hash = Objects.hash(item, tag);
        }

        static ItemKey of(ItemStack stack) {
            return new ItemKey(stack.getItem(), stack.getTag());
        }

        static ItemKey owned(ItemStack stack) {
            CompoundTag t = stack.getTag();
            return new ItemKey(stack.getItem(), t != null ? t.copy() : null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemKey other)) return false;
            return item == other.item && Objects.equals(tag, other.tag);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
