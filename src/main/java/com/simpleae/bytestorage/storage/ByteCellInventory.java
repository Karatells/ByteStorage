package com.simpleae.bytestorage.storage;

import com.simpleae.bytestorage.api.storage.CellTier;
import com.simpleae.bytestorage.api.storage.ICellInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private final ItemStack cellStack;
    private final CellTier tier;
    private final List<ItemStack> storedItems;
    private int cachedUsedBytes;

    public ByteCellInventory(ItemStack cellStack, CellTier tier) {
        this.cellStack = cellStack;
        this.tier = tier;
        this.storedItems = new ArrayList<>();
        this.loadFromTag();
    }

    /**
     * Завантажує список предметів з NBT диска.
     * Зауважте: використовується поле "ByteCount" типу int, оскільки
     * ванільний ItemStack.save() записує Count як байт (макс. 127),
     * а нам потрібна підтримка тисяч предметів в одній позиції.
     */
    private void loadFromTag() {
        storedItems.clear();
        cachedUsedBytes = 0;

        CompoundTag rootTag = cellStack.getTag();
        if (rootTag == null || !rootTag.contains(TAG_INVENTORY, Tag.TAG_LIST)) {
            return;
        }

        ListTag listTag = rootTag.getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag entryTag = listTag.getCompound(i);
            ItemStack stack = ItemStack.of(entryTag);

            if (!stack.isEmpty()) {
                if (entryTag.contains(TAG_BYTE_COUNT, Tag.TAG_INT)) {
                    stack.setCount(entryTag.getInt(TAG_BYTE_COUNT));
                }
                storedItems.add(stack);
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
        for (ItemStack stack : storedItems) {
            if (!stack.isEmpty()) {
                CompoundTag entryTag = stack.save(new CompoundTag());
                entryTag.putInt(TAG_BYTE_COUNT, stack.getCount());
                listTag.add(entryTag);
                totalBytes += stack.getCount();
            }
        }

        rootTag.put(TAG_INVENTORY, listTag);
        rootTag.putInt(TAG_USED_BYTES, totalBytes);
        this.cachedUsedBytes = totalBytes;
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
        return Collections.unmodifiableList(storedItems);
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
            boolean merged = false;
            // Шукаємо, чи такий предмет вже є (ідентичний предмет + однаковий NBT)
            for (ItemStack existing : storedItems) {
                if (ItemStack.isSameItemSameTags(existing, toInsert)) {
                    existing.grow(insertCount);
                    merged = true;
                    break;
                }
            }

            // Якщо такого предмета ще немає, додаємо новий запис (немає ліміту на унікальні типи!)
            if (!merged) {
                ItemStack newEntry = toInsert.copy();
                newEntry.setCount(insertCount);
                storedItems.add(newEntry);
            }

            saveToTag();
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

        for (int i = 0; i < storedItems.size(); i++) {
            ItemStack existing = storedItems.get(i);
            if (ItemStack.isSameItemSameTags(existing, toExtract)) {
                int extractCount = Math.min(existing.getCount(), amount);
                ItemStack extractedStack = existing.copy();
                extractedStack.setCount(extractCount);

                if (!simulate) {
                    existing.shrink(extractCount);
                    if (existing.isEmpty()) {
                        storedItems.remove(i);
                    }
                    saveToTag();
                }

                return extractedStack;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void clear() {
        storedItems.clear();
        saveToTag();
    }
}
