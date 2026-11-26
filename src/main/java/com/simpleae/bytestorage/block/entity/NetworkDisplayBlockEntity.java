package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.menu.NetworkDisplayMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/** BlockEntity Інформаційного монітора мережі (6 рядків тексту, керованих зі скриптів). */
public class NetworkDisplayBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    public static final int MAX_LINES = 6;
    private IGrid grid;
    private final String[] lines = new String[MAX_LINES];

    public NetworkDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.NETWORK_DISPLAY_BLOCK_ENTITY.get(), pos, state);
        Arrays.fill(lines, "");
    }

    public String getLine(int index) {
        if (index < 0 || index >= MAX_LINES) return "";
        return lines[index];
    }

    public void setLine(int index, String text) {
        if (index < 0 || index >= MAX_LINES) return;
        String clean = text == null ? "" : (text.length() > 128 ? text.substring(0, 128) : text);
        if (!clean.equals(lines[index])) {
            lines[index] = clean;
            setChanged();
            syncToClient();
        }
    }

    public void clearLines() {
        Arrays.fill(lines, "");
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
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
        return 2;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.network_display");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NetworkDisplayMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag lt = new ListTag();
        for (String l : lines) {
            lt.add(StringTag.valueOf(l));
        }
        tag.put("Lines", lt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        Arrays.fill(lines, "");
        ListTag lt = tag.getList("Lines", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(MAX_LINES, lt.size()); i++) {
            lines[i] = lt.getString(i);
        }
    }
}
