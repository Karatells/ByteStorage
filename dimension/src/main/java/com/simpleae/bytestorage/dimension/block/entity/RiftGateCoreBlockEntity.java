package com.simpleae.bytestorage.dimension.block.entity;

import com.simpleae.bytestorage.dimension.DimensionConfig;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ядро Врат Разлома — хранит Forge Energy (по умолчанию 100 000 FE) для активации портала.
 */
public class RiftGateCoreBlockEntity extends BlockEntity {

    private static final int MAX_RECEIVE = 5_000;

    private final EnergyStorage energy = new EnergyStorage(100_000, MAX_RECEIVE, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = super.receiveEnergy(maxReceive, simulate);
            if (accepted > 0 && !simulate) {
                setChanged();
            }
            return accepted;
        }
    };

    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.empty();

    public RiftGateCoreBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleAeDimension.RIFT_GATE_CORE_ENTITY.get(), pos, state);
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getRequiredEnergy() {
        return Math.min(energy.getMaxEnergyStored(), DimensionConfig.PORTAL_ACTIVATION_ENERGY.get());
    }

    public boolean hasEnoughEnergy() {
        return energy.getEnergyStored() >= getRequiredEnergy();
    }

    public boolean consumeActivationEnergy() {
        int req = getRequiredEnergy();
        if (energy.getEnergyStored() < req) {
            return false;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", energy.getEnergyStored() - req);
        energy.deserializeNBT(tag.get("Energy"));
        setChanged();
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        energyCap = LazyOptional.of(() -> energy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Energy")) {
            energy.deserializeNBT(tag.get("Energy"));
        }
    }
}
