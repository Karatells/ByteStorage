package com.simpleae.bytestorage.scripting.block.entity;

import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.scripting.SimpleAeScripting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Логічний термінал: вузол мережі, через який редагуються скрипти Логічних шин. */
public class LogicTerminalBlockEntity extends BlockEntity implements IGridNode {

    private IGrid grid;

    public LogicTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleAeScripting.LOGIC_TERMINAL_ENTITY.get(), pos, state);
    }

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
        return 2;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }
}
