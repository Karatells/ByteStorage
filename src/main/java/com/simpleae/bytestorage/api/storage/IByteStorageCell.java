package com.simpleae.bytestorage.api.storage;

import net.minecraft.world.item.ItemStack;

/**
 * Інтерфейс для предметів-дисків (Storage Cell Items).
 * Будь-який предмет, що реалізує цей інтерфейс, вважається диском у SimpleAE.
 */
public interface IByteStorageCell {

    /**
     * Рівень (Tier) даного диска.
     */
    CellTier getTier(ItemStack stack);

    /**
     * Максимальна ємність диска в байтах (предметах).
     */
    default int getMaxBytes(ItemStack stack) {
        return getTier(stack).getCapacityBytes();
    }

    /**
     * Поточна кількість зайнятих байтів у диску.
     */
    int getUsedBytes(ItemStack stack);

    /**
     * Отримати обгортку інвентаря над ItemStack диска.
     */
    ICellInventory getCellInventory(ItemStack stack);

    /**
     * Перевірка, чи можна вставити хоча б 1 байт.
     */
    default boolean hasSpace(ItemStack stack) {
        return getUsedBytes(stack) < getMaxBytes(stack);
    }
}
