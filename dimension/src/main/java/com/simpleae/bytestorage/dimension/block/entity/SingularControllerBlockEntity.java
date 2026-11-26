package com.simpleae.bytestorage.dimension.block.entity;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.block.entity.CraftingCoprocessorBlockEntity;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Сингулярный контроллер (узел ядра эндгейм-сети):
 * - Буфер 10,000,000 FE (приём до 100,000 FE/t, мгновенная подпитка Контроллера сети).
 * - Пассивная квантовая генерация +250 FE/t, когда подключён к активной сети.
 * - Действует как +4 сопроцессора крафта для всех Асемблеров сети.
 */
public class SingularControllerBlockEntity extends CraftingCoprocessorBlockEntity {

    public static final int CAPACITY = 10_000_000;
    public static final int RECEIVE_PER_TICK = 100_000;
    private static final int PASSIVE_GEN_PER_CYCLE = 1_250; // 250 FE/t * 5 ticks
    private static final int CYCLE_TICKS = 5;

    private int stored = 0;
    private int tickCounter = 0;

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
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
            int extracted = Math.max(0, Math.min(Math.min(maxExtract, RECEIVE_PER_TICK), stored));
            if (!simulate && extracted > 0) {
                stored -= extracted;
                setChanged();
            }
            return extracted;
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
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> energyStorage);

    public SingularControllerBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleAeDimension.SINGULAR_CONTROLLER_ENTITY.get(), pos, state);
    }

    public int getStoredEnergy() {
        return stored;
    }

    @Override
    public int getCoprocessorPower() {
        return 4;
    }

    @Override
    public int getIdleEnergyDrain() {
        return 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SingularControllerBlockEntity be) {
        if (++be.tickCounter % CYCLE_TICKS != 0) {
            return;
        }
        // Квантовый резонанс: мягкая пассивная подпитка буфера
        if (be.stored < CAPACITY) {
            be.stored = Math.min(CAPACITY, be.stored + PASSIVE_GEN_PER_CYCLE);
            be.setChanged();
        }
        IGrid grid = be.getGrid();
        if (grid == null) return;
        NetworkControllerBlockEntity controller = grid.getController();
        if (controller == null) return;

        controller.getCapability(ForgeCapabilities.ENERGY).ifPresent(ctrlEnergy -> {
            if (be.stored > 0 && ctrlEnergy.canReceive()) {
                int toSend = Math.min(be.stored, RECEIVE_PER_TICK * CYCLE_TICKS);
                int accepted = ctrlEnergy.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    be.stored -= accepted;
                    be.setChanged();
                }
            }
        });
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", stored);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stored = Math.max(0, Math.min(CAPACITY, tag.getInt("Energy")));
    }
}
