package com.simpleae.bytestorage.api.storage;

import com.simpleae.bytestorage.api.network.IGridNode;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

/**
 * Інтерфейс для пристроїв, що вміщують диски зберігання (наприклад, ME Drive / Drive Block).
 */
public interface IDriveHost extends IGridNode {

    /**
     * Повертає список активних інвентарів усіх дисків, вставлених у цей блок.
     */
    List<ICellInventory> getActiveCellInventories();

    /**
     * Фізичний інвентар слотів для самих ячеєк (дисків).
     */
    IItemHandler getDriveInventory();

    /**
     * Кількість слотів для дисків у накопичувачі (зазвичай 10).
     */
    int getCellCount();

    /**
     * Викликається після того, як вміст ячеєк було змінено (вставка/вилучення предметів),
     * щоб блок міг позначити себе «брудним» і збережений NBT дисків потрапив на диск.
     */
    default void onCellContentsChanged() {
    }

    /**
     * Пріоритет вставки: накопичувачі з вищим пріоритетом заповнюються першими.
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Білий список: чи може цей накопичувач приймати предмет (порожній список = приймає все).
     */
    default boolean acceptsItem(net.minecraft.world.item.ItemStack stack) {
        return true;
    }
}
