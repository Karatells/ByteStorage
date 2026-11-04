package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.item.RecipeLibraryItem;
import com.simpleae.bytestorage.item.WirelessTerminalItem;
import com.simpleae.bytestorage.network.packet.ControllerStatsPacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.util.NetworkStats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Меню Контролера мережі: слот привязки беспроводного терминала, инвентарь игрока и
 * периодически синхронизируемая статистика сети.
 */
public class ControllerMenu extends AbstractContainerMenu {

    public static final int WIRELESS_X = 8;
    public static final int WIRELESS_Y = 124;
    public static final int LIBRARY_X = 8;
    public static final int LIBRARY_Y = 152;
    public static final int PLAYER_START = 2;
    public static final int PLAYER_INV_X = 47;
    public static final int PLAYER_INV_Y = 188;

    private final NetworkControllerBlockEntity controller;
    private NetworkStats stats = new NetworkStats();
    private int syncTimer = 0;
    private final Player inventoryPlayer;

    public ControllerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (NetworkControllerBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ControllerMenu(int containerId, Inventory playerInventory, NetworkControllerBlockEntity controller) {
        super(SimpleByteStorage.CONTROLLER_MENU.get(), containerId);
        this.controller = controller;
        this.inventoryPlayer = playerInventory.player;

        this.addSlot(new SlotItemHandler(controller.getWirelessSlot(), 0, WIRELESS_X, WIRELESS_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof WirelessTerminalItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new SlotItemHandler(controller.getLibrarySlot(), 0, LIBRARY_X, LIBRARY_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof RecipeLibraryItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public NetworkStats getStats() {
        return stats;
    }

    public void setStats(NetworkStats stats) {
        this.stats = stats;
    }

    public ItemStack getWirelessStack() {
        return this.slots.get(0).getItem();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (controller != null && inventoryPlayer instanceof ServerPlayer serverPlayer && (syncTimer++ % 10 == 0)) {
            SimpleByteStoragePackets.sendToPlayer(new ControllerStatsPacket(NetworkStats.collect(controller)), serverPlayer);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!this.moveItemStackTo(stack, PLAYER_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof WirelessTerminalItem) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof RecipeLibraryItem) {
            if (!this.moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (controller == null) {
            return false;
        }
        Level level = controller.getNodeLevel();
        return level != null
                && level.getBlockEntity(controller.getNodePos()) == controller
                && player.distanceToSqr(controller.getNodePos().getX() + 0.5, controller.getNodePos().getY() + 0.5,
                controller.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
