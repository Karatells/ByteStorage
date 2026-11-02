package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.IPatternStorage;
import com.simpleae.bytestorage.menu.PatternTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity Терміналу Шаблонів (Pattern Terminal).
 * 
 * Точка доступу до центрального сховища шаблонів крафту мережі (IPatternStorage).
 * Завдяки цьому всі асемблери мережі можуть використовувати спільні шаблони
 * для автоматизованого крафту.
 */
public class PatternTerminalBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    private IGrid grid;

    public PatternTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.PATTERN_TERMINAL_BLOCK_ENTITY.get(), pos, state);
    }

    // --- IGridNode ---

    @Override
    @Nullable
    public IGrid getGrid() {
        return grid;
    }

    @Override
    public void setGrid(@Nullable IGrid grid) {
        this.grid = grid;
    }

    @Override
    public BlockPos getNodePos() {
        return this.worldPosition;
    }

    @Override
    public Level getNodeLevel() {
        return this.level;
    }

    @Override
    public int getIdleEnergyDrain() {
        return 1;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    /**
     * Отримати сховище шаблонов крафту поточної мережі.
     */
    public IPatternStorage getPatternStorage() {
        return grid != null ? grid.getPatternStorage() : null;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.pattern_terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PatternTerminalMenu(containerId, playerInventory, this);
    }
}
