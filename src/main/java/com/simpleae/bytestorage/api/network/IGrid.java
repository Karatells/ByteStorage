package com.simpleae.bytestorage.api.network;

import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * Інтерфейс загальної мережі SimpleAE (аналог ME Grid).
 * Об'єднує всі ноди, керує живленням та доступом до віртуального сховища.
 */
public interface IGrid {

    /**
     * Унікальний ідентифікатор мережі.
     */
    UUID getGridId();

    /**
     * Поточний статус роботи мережі.
     */
    GridState getState();

    /**
     * Змінити стан мережі.
     */
    void setState(GridState newState);

    /**
     * Контролер, що живить і координує мережу.
     */
    @Nullable
    NetworkControllerBlockEntity getController();

    /**
     * Віртуальне сховище мережі (пул предметів і байтовий менеджер).
     */
    INetworkStorage getStorage();

    /**
     * Сховище шаблонів крафту мережі (Pattern Storage).
     * Зберігає всі рецепти-шаблони, доступні для автокрафту.
     */
    IPatternStorage getPatternStorage();

    /**
     * Всі підключені пристрої та кабелі.
     */
    Collection<IGridNode> getNodes();

    /**
     * Додати ноду до мережі.
     */
    void addNode(IGridNode node);

    /**
     * Видалити ноду з мережі.
     */
    void removeNode(IGridNode node);

    /**
     * Загальне споживання енергії мережею за тік (FE/t).
     */
    int getTotalEnergyDrain();

    /**
     * Перевірка цілісності та конфліктів у мережі (наприклад, більше ніж один контролер).
     */
    void validateNetwork();
}
