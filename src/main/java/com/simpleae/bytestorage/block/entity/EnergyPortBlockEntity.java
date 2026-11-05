package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.util.NetworkPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Узел сети без интерфейса: раз в 5 тиков заряжает соседние блоки с энергией (FE) из буфера контроллера. */
public class EnergyPortBlockEntity extends BlockEntity implements IGridNode {

    private static final int CYCLE_TICKS = 5;

    private IGrid grid;
    private int tickCounter = 0;

    public EnergyPortBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.ENERGY_PORT_BLOCK_ENTITY.get(), pos, state);
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
        return 1;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyPortBlockEntity port) {
        if (++port.tickCounter % CYCLE_TICKS == 0) {
            NetworkPower.feed(port.grid, level, pos, CYCLE_TICKS);
        }
    }
}
