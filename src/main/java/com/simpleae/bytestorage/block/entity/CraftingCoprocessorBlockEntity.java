package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** BlockEntity Сопроцесора крафту: додає паралельну обчислювальну потужність автокрафту мережі. */
public class CraftingCoprocessorBlockEntity extends BlockEntity implements IGridNode {

    private IGrid grid;

    public CraftingCoprocessorBlockEntity(BlockPos pos, BlockState state) {
        this(SimpleByteStorage.CRAFTING_COPROCESSOR_BLOCK_ENTITY.get(), pos, state);
    }

    protected CraftingCoprocessorBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getCoprocessorPower() {
        return 1;
    }

    public static int countOnGrid(@Nullable IGrid grid) {
        if (grid == null) return 0;
        int count = 0;
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof CraftingCoprocessorBlockEntity cp) {
                count += cp.getCoprocessorPower();
            }
        }
        return count;
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
        return 8;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }
}
