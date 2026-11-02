package com.simpleae.bytestorage.api.storage;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * Інтерфейс для інвентаря байтової ячейки зберігання.
 * 
 * Ключова відмінність:
 * Немає обмежень на кількість слотів/типів предметів (Types).
 * 1 предмет = 1 байт.
 */
public interface ICellInventory {

    /**
     * Загальна максимальна ємність ячейки в байтах.
     */
    int getMaxBytes();

    /**
     * Поточна кількість зайнятих байтів (сума всіх предметів).
     * Формула: sum(item.getCount())
     */
    int getUsedBytes();

    /**
     * Кількість вільних байтів для запису.
     */
    default int getFreeBytes() {
        return Math.max(0, getMaxBytes() - getUsedBytes());
    }

    /**
     * Повертає список усіх предметів, що зберігаються в ячейці.
     */
    List<ItemStack> getStoredItems();

    /**
     * Вставка предметів у ячейку.
     * 
     * @param toInsert Предмет для вставки
     * @param simulate Якщо true, лише симулює операцію без зміни NBT
     * @return Залишок предмета, який не помістився (ItemStack.EMPTY, якщо помістився повністю)
     */
    ItemStack insertItem(ItemStack toInsert, boolean simulate);

    /**
     * Вилучення предмета з ячейки.
     * 
     * @param toExtract Шаблон предмета для вилучення (порівнюється предмет і NBT)
     * @param amount Кількість для вилучення
     * @param simulate Якщо true, лише симулює операцію
     * @return Фактично вилучений стак
     */
    ItemStack extractItem(ItemStack toExtract, int amount, boolean simulate);

    /**
     * Перевірка, чи заповнена ячейка.
     */
    default boolean isFull() {
        return getUsedBytes() >= getMaxBytes();
    }

    /**
     * Перевірка, чи порожня ячейка.
     */
    default boolean isEmpty() {
        return getUsedBytes() == 0;
    }

    /**
     * Очистити всі предмети з ячейки (після виконання крафту).
     */
    void clear();
}
