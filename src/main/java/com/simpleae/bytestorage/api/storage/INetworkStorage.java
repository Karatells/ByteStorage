package com.simpleae.bytestorage.api.storage;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Головний інтерфейс логіки віртуального сховища мережі (Virtual Item Pool).
 * 
 * На відміну від стандартних ванільних інвентарів зі слотами 0..N,
 * INetworkStorage представляє всю мережу як єдиний абстрактний басейн предметів.
 */
public interface INetworkStorage {

    /**
     * Загальна ємність усіх підключених ячеєк у байтах (1 байт = 1 предмет).
     */
    int getTotalCapacityBytes();

    /**
     * Сумарно використані байти по всій мережі.
     */
    int getTotalUsedBytes();

    /**
     * Вільна кількість байт для запису нових предметів.
     */
    default int getRemainingBytes() {
        return Math.max(0, getTotalCapacityBytes() - getTotalUsedBytes());
    }

    /**
     * Повертає зведений список усіх предметів у мережі.
     * Якщо один і той самий предмет лежить у різних дисках, тут він об'єднаний
     * у сумарну кількість.
     */
    List<ItemStack> getAllItems();

    /**
     * Вставка предметів у віртуальний пул сховища.
     * 
     * @param stack Предмет для вставки
     * @param simulate Якщо true, лише розраховує результат без зміни вмісту дисків
     * @return Залишок предмета, який не помістився через брак вільного місця
     */
    ItemStack insertItem(ItemStack stack, boolean simulate);

    /**
     * Вилучення предмета за зразком ItemStack (враховує Item та NBT/пошкодження).
     * 
     * @param template Зразок предмета
     * @param count Бажана кількість
     * @param simulate Якщо true, лише перевіряє можливість вилучення
     * @return Реально вилучений ItemStack
     */
    ItemStack extractItem(ItemStack template, int count, boolean simulate);

    /**
     * Спрощений метод вилучення за типом предмета (Item).
     * 
     * @param item Тип предмета
     * @param count Кількість
     * @param simulate Якщо true, симуляція
     * @return Реально вилучений ItemStack
     */
    ItemStack extractItem(Item item, int count, boolean simulate);

    /**
     * Реєстрація накопичувача дисків (Drive) у віртуальному сховищі.
     */
    void attachDrive(IDriveHost drive);

    /**
     * Відключення накопичувача дисків від сховища.
     */
    void detachDrive(IDriveHost drive);

    /**
     * Примусове скидання кешу після зміни складу дисків або предметів.
     */
    void invalidateCache();
}
