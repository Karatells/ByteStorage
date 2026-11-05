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
    public static final int BLACKLIST_START = 2;
    public static final int PREFS_START = BLACKLIST_START + NetworkControllerBlockEntity.RECIPE_SETTING_SLOTS;
    public static final int PLAYER_START = PREFS_START + NetworkControllerBlockEntity.RECIPE_SETTING_SLOTS;
    public static final int RECIPES_TAB = 3;
    public static final int GHOST_X = 10;
    public static final int BLACKLIST_Y = 54;
    public static final int PREFS_Y = 84;
    public static final int PLAYER_INV_X = 47;
    public static final int PLAYER_INV_Y = 188;

    private final NetworkControllerBlockEntity controller;
    private NetworkStats stats = new NetworkStats();
    private int syncTimer = 0;
    private final Player inventoryPlayer;
    private int tab = 0;

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
        addGhostRow(controller.getRecipeBlacklist(), BLACKLIST_Y);
        addGhostRow(controller.getRecipePrefs(), PREFS_Y);
        this.addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return controller.isSkipDecompress() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                skipClient = value != 0;
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

    private boolean skipClient = true;

    private void addGhostRow(net.minecraftforge.items.IItemHandler handler, int y) {
        for (int i = 0; i < handler.getSlots(); i++) {
            this.addSlot(new SlotItemHandler(handler, i, GHOST_X + i * 18, y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return tab == RECIPES_TAB;
                }
            });
        }
    }

    public int getTab() {
        return tab;
    }

    public void setTab(int tab) {
        this.tab = tab;
    }

    public boolean isSkipDecompress() {
        return skipClient;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id == 0 && !p.level().isClientSide && controller != null) {
            controller.setSkipDecompress(!controller.isSkipDecompress());
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player p) {
        if (slotId >= BLACKLIST_START && slotId < PLAYER_START
                && (clickType == net.minecraft.world.inventory.ClickType.PICKUP
                || clickType == net.minecraft.world.inventory.ClickType.QUICK_MOVE
                || clickType == net.minecraft.world.inventory.ClickType.SWAP)) {
            Slot slot = this.slots.get(slotId);
            ItemStack carried = this.getCarried();
            ItemStack source;
            if (clickType == net.minecraft.world.inventory.ClickType.QUICK_MOVE
                    || clickType == net.minecraft.world.inventory.ClickType.PICKUP && carried.isEmpty()) {
                source = ItemStack.EMPTY;
            } else if (clickType == net.minecraft.world.inventory.ClickType.SWAP) {
                source = (button >= 0 && button < 9) ? p.getInventory().getItem(button) : ItemStack.EMPTY;
            } else {
                source = carried;
            }
            if (source.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                ItemStack ghost = source.copy();
                ghost.setCount(1);
                slot.set(ghost);
            }
            return;
        }
        super.clicked(slotId, button, clickType, p);
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
