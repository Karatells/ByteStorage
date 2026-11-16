package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.menu.SecurityTerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** BlockEntity Термінала безпеки: зберігає власника мережі та стан блокування доступу. */
public class SecurityTerminalBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    private IGrid grid;
    private UUID ownerUuid;
    private String ownerName = "";
    private boolean locked = true;

    public SecurityTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.SECURITY_TERMINAL_BLOCK_ENTITY.get(), pos, state);
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        setChanged();
    }

    public boolean isOwner(Player player) {
        if (ownerUuid == null) return true;
        return ownerUuid.equals(player.getUUID()) || player.hasPermissions(2);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isLocked() {
        return locked;
    }

    public void toggleLocked() {
        this.locked = !this.locked;
        setChanged();
    }

    /** Перевіряє, чи має гравець доступ до терміналів даної мережі. */
    public static boolean canAccess(@Nullable IGrid grid, Player player) {
        if (grid == null || player == null) {
            return true;
        }
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof SecurityTerminalBlockEntity sec && sec.isLocked() && !sec.isOwner(player)) {
                player.displayClientMessage(Component.translatable("msg.simple_bytestorage.security.denied")
                        .withStyle(ChatFormatting.RED), true);
                return false;
            }
        }
        return true;
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
        return 4;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.security_terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SecurityTerminalMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerUuid != null) {
            tag.putUUID("OwnerUuid", ownerUuid);
        }
        tag.putString("OwnerName", ownerName);
        tag.putBoolean("Locked", locked);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ownerUuid = tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : null;
        ownerName = tag.getString("OwnerName");
        locked = !tag.contains("Locked") || tag.getBoolean("Locked");
    }
}
