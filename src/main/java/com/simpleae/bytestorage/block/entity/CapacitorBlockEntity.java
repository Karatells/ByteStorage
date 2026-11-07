package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.util.CapacitorLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Конденсатор: узел сети с собственным буфером энергии. Когда Контроллер почти разряжен — подпитывает его,
 * когда переполнен — забирает излишек. Снаружи принимает энергию (генераторы рядом), но не отдаёт.
 */
public class CapacitorBlockEntity extends BlockEntity implements IGridNode {

    public static final int CAPACITY = 500_000;
    public static final int RECEIVE_PER_TICK = 2_000;
    public static final int TRANSFER_PER_TICK = 4_000;
    private static final int CYCLE_TICKS = 5;

    private IGrid grid;
    private int tickCounter = 0;
    private int stored = 0;

    private final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.max(0, Math.min(Math.min(maxReceive, RECEIVE_PER_TICK), CAPACITY - stored));
            if (!simulate && accepted > 0) {
                stored += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };
    private final LazyOptional<IEnergyStorage> handler = LazyOptional.of(() -> storage);

    public CapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.CAPACITOR_BLOCK_ENTITY.get(), pos, state);
    }

    public int getStored() {
        return stored;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, CapacitorBlockEntity cap) {
        if (++cap.tickCounter % CYCLE_TICKS != 0 || cap.grid == null || !cap.grid.getState().isActive()) {
            return;
        }
        NetworkControllerBlockEntity controller = cap.grid.getController();
        if (controller == null) {
            return;
        }
        int move = CapacitorLogic.transfer(controller.getEnergyStored(), controller.getMaxEnergyStored(),
                cap.stored, CAPACITY, TRANSFER_PER_TICK * CYCLE_TICKS);
        if (move > 0) {
            cap.stored -= move;
            controller.refundEnergy(move);
            cap.setChanged();
        } else if (move < 0) {
            int taken = controller.drawEnergy(-move);
            cap.stored += taken;
            cap.setChanged();
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Stored", stored);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stored = Math.max(0, Math.min(CAPACITY, tag.getInt("Stored")));
    }
}
