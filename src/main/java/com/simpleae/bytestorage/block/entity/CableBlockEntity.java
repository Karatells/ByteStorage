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

/**
 * Нода-кабель: лише проводить мережу (споживання енергії 0).
 */
public class CableBlockEntity extends BlockEntity implements IGridNode {

    private IGrid grid;
    @Nullable
    private BlockState facade;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.CABLE_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public BlockState getFacade() {
        return facade;
    }

    public void setFacade(@Nullable BlockState facade) {
        this.facade = facade;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        if (facade != null) {
            tag.put("Facade", net.minecraft.nbt.NbtUtils.writeBlockState(facade));
        }
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Facade")) {
            facade = net.minecraft.nbt.NbtUtils.readBlockState(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Facade"));
            if (facade.isAir()) facade = null;
        } else {
            facade = null;
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
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
        return 0;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
    }
}
