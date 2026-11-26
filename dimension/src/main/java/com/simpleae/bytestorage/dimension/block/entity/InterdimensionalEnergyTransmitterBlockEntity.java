package com.simpleae.bytestorage.dimension.block.entity;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Межпространственный передатчик энергии (16 каналов):
 * - Буфер 1,000,000 FE, передача до 20,000 FE/t.
 * - Автоматически выравнивает заряд между всеми передатчиками на одном канале во ВСЕХ измерениях
 *   (Обычный мир <-> Кремниевая плоскость <-> Незер <-> Энд) и отдаёт FE соседним машинам.
 */
public class InterdimensionalEnergyTransmitterBlockEntity extends BlockEntity {

    public static final int CAPACITY = 1_000_000;
    public static final int IO_RATE = 20_000;
    public static final int MAX_CHANNELS = 16;
    private static final int CYCLE_TICKS = 5;

    private static final Map<Integer, Set<InterdimensionalEnergyTransmitterBlockEntity>> CHANNELS =
            new ConcurrentHashMap<>();

    private int channel = 1;
    private int stored = 0;
    private int tickCounter = 0;
    private boolean registered = false;

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.max(0, Math.min(Math.min(maxReceive, IO_RATE), CAPACITY - stored));
            if (!simulate && accepted > 0) {
                stored += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.max(0, Math.min(Math.min(maxExtract, IO_RATE), stored));
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

    public InterdimensionalEnergyTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleAeDimension.ENERGY_TRANSMITTER_ENTITY.get(), pos, state);
    }

    public int getChannel() {
        return channel;
    }

    public int getStoredEnergy() {
        return stored;
    }

    public int getLinkedCount() {
        Set<InterdimensionalEnergyTransmitterBlockEntity> peers = CHANNELS.get(channel);
        return peers == null ? 1 : peers.size();
    }

    public int cycleChannel() {
        unregister();
        channel = (channel % MAX_CHANNELS) + 1;
        register();
        setChanged();
        return channel;
    }

    private void register() {
        if (level == null || level.isClientSide || registered) return;
        CHANNELS.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(this);
        registered = true;
    }

    private void unregister() {
        if (!registered) return;
        Set<InterdimensionalEnergyTransmitterBlockEntity> set = CHANNELS.get(channel);
        if (set != null) {
            set.remove(this);
        }
        registered = false;
    }

    @Override
    public void setRemoved() {
        unregister();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unregister();
        super.onChunkUnloaded();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InterdimensionalEnergyTransmitterBlockEntity be) {
        if (!be.registered) {
            be.register();
        }
        if (++be.tickCounter % CYCLE_TICKS != 0) {
            return;
        }

        // 1. Межпространственное выравнивание энергии по каналу (push и pull)
        Set<InterdimensionalEnergyTransmitterBlockEntity> peers = CHANNELS.get(be.channel);
        if (peers != null && peers.size() > 1) {
            int maxTransfer = IO_RATE * CYCLE_TICKS;
            for (InterdimensionalEnergyTransmitterBlockEntity peer : peers) {
                if (peer == be || peer.isRemoved() || peer.level == null) continue;
                int diff = be.stored - peer.stored;
                if (diff > 1) {
                    int move = Math.min(diff / 2, Math.min(maxTransfer, CAPACITY - peer.stored));
                    if (move > 0) {
                        be.stored -= move;
                        peer.stored += move;
                        be.setChanged();
                        peer.setChanged();
                        maxTransfer -= move;
                        if (maxTransfer <= 0) break;
                    }
                } else if (diff < -1) {
                    int move = Math.min((-diff) / 2, Math.min(maxTransfer, CAPACITY - be.stored));
                    if (move > 0) {
                        peer.stored -= move;
                        be.stored += move;
                        peer.setChanged();
                        be.setChanged();
                        maxTransfer -= move;
                        if (maxTransfer <= 0) break;
                    }
                }
            }
        }

        // 2. Отдача энергии соседним приёмникам FE
        if (be.stored > 0) {
            int budget = Math.min(be.stored, IO_RATE * CYCLE_TICKS);
            for (Direction dir : Direction.values()) {
                if (budget <= 0) break;
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null || neighbor instanceof InterdimensionalEnergyTransmitterBlockEntity) continue;
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(target -> {
                    if (target.canReceive() && be.stored > 0) {
                        int sent = target.receiveEnergy(Math.min(be.stored, IO_RATE * CYCLE_TICKS), false);
                        if (sent > 0) {
                            be.stored -= sent;
                            be.setChanged();
                        }
                    }
                });
            }
        }
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
        tag.putInt("Channel", channel);
        tag.putInt("Energy", stored);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        channel = Math.max(1, Math.min(MAX_CHANNELS, tag.contains("Channel") ? tag.getInt("Channel") : 1));
        stored = Math.max(0, Math.min(CAPACITY, tag.getInt("Energy")));
    }
}
