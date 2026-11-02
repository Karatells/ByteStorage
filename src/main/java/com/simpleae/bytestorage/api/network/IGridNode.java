package com.simpleae.bytestorage.api.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Інтерфейс для будь-якого компонента (ноди) мережі SimpleAE:
 * кабелі, контролер, накопичувачі дисків (Drive), термінали тощо.
 */
public interface IGridNode {

    /**
     * Поточна мережа, до якої підключена дана нода.
     */
    @Nullable
    IGrid getGrid();

    /**
     * Встановлює прив'язку до мережі.
     */
    void setGrid(@Nullable IGrid grid);

    /**
     * Позиція ноди у світі.
     */
    BlockPos getNodePos();

    /**
     * Рівень (світ), у якому знаходиться нода.
     */
    Level getNodeLevel();

    /**
     * Пасивне споживання енергії цим пристроєм за тік (FE/t).
     */
    default int getIdleEnergyDrain() {
        return 1;
    }

    /**
     * Викликається при зміні стану мережі (OFFLINE -> ONLINE і навпаки).
     */
    void onGridStateChanged(GridState newState);

    /**
     * Чи є дана нода контролером мережі.
     */
    default boolean isController() {
        return false;
    }
}
