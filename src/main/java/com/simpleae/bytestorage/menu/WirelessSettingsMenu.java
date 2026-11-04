package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.item.WirelessConfig;
import com.simpleae.bytestorage.item.WirelessTerminalItem;
import com.simpleae.bytestorage.item.WirelessUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Настройки Беспроводного терминала: 2 слота улучшений, 9 слотов-призраков фильтра, режимы.
 * Кнопки: 0 — автопополнение, 1 — автосбор, 2 — белый/чёрный список.
 * Терминал берётся из слота инвентаря игрока (индекс передаётся при открытии); этот слот заблокирован.
 */
public class WirelessSettingsMenu extends AbstractContainerMenu {

    public static final int UPGRADE_X = 8;
    public static final int UPGRADE_Y = 22;
    public static final int FILTER_X = 8;
    public static final int FILTER_Y = 76;
    public static final int PLAYER_INV_Y = 108;
    public static final int PLAYER_START = WirelessConfig.TOTAL_SLOTS;

    private final Player player;
    private final int terminalSlot;
    private final ItemStackHandler handler;
    private final ContainerData data;

    public WirelessSettingsMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, buf.readVarInt());
    }

    public WirelessSettingsMenu(int containerId, Inventory inv, int terminalSlot) {
        super(SimpleByteStorage.WIRELESS_SETTINGS_MENU.get(), containerId);
        this.player = inv.player;
        this.terminalSlot = terminalSlot;
        ItemStack terminal = terminal();
        this.handler = new ItemStackHandler(WirelessConfig.TOTAL_SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                persist();
            }
        };
        if (terminal.getItem() instanceof WirelessTerminalItem) {
            ItemStackHandler loaded = WirelessConfig.load(terminal);
            for (int i = 0; i < WirelessConfig.TOTAL_SLOTS; i++) {
                handler.setStackInSlot(i, loaded.getStackInSlot(i));
            }
        }

        this.data = player.level().isClientSide ? new SimpleContainerData(3) : new ContainerData() {
            @Override
            public int get(int index) {
                ItemStack t = terminal();
                return switch (index) {
                    case 0 -> WirelessConfig.restock(t) ? 1 : 0;
                    case 1 -> WirelessConfig.pickup(t) ? 1 : 0;
                    default -> WirelessConfig.whitelist(t) ? 1 : 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
        this.addDataSlots(this.data);

        for (int i = 0; i < WirelessConfig.UPGRADE_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(handler, i, UPGRADE_X + i * 18, UPGRADE_Y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof WirelessUpgradeItem;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int i = 0; i < WirelessConfig.FILTER_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(handler, WirelessConfig.UPGRADE_SLOTS + i, FILTER_X + i * 18, FILTER_Y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = col + row * 9 + 9;
                this.addSlot(new Slot(inv, idx, 8 + col * 18, PLAYER_INV_Y + row * 18) {
                    @Override
                    public boolean mayPickup(Player p) {
                        return idx != terminalSlot;
                    }

                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return idx != terminalSlot;
                    }
                });
            }
        }
        for (int col = 0; col < 9; col++) {
            int idx = col;
            this.addSlot(new Slot(inv, idx, 8 + col * 18, PLAYER_INV_Y + 58) {
                @Override
                public boolean mayPickup(Player p) {
                    return idx != terminalSlot;
                }

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return idx != terminalSlot;
                }
            });
        }
    }

    private ItemStack terminal() {
        if (terminalSlot < 0 || terminalSlot >= player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(terminalSlot);
    }

    private void persist() {
        if (player.level().isClientSide) return;
        ItemStack t = terminal();
        if (t.getItem() instanceof WirelessTerminalItem) {
            WirelessConfig.save(t, handler);
        }
    }

    public boolean isRestock() {
        return data.get(0) != 0;
    }

    public boolean isPickup() {
        return data.get(1) != 0;
    }

    public boolean isWhitelist() {
        return data.get(2) != 0;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (p.level().isClientSide) return true;
        ItemStack t = terminal();
        if (!(t.getItem() instanceof WirelessTerminalItem)) return false;
        switch (id) {
            case 0 -> WirelessConfig.setFlag(t, "Restock", !WirelessConfig.restock(t));
            case 1 -> WirelessConfig.setFlag(t, "Pickup", !WirelessConfig.pickup(t));
            case 2 -> WirelessConfig.setFlag(t, "Whitelist", !WirelessConfig.whitelist(t));
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player p) {
        int filterStart = WirelessConfig.UPGRADE_SLOTS;
        int filterEnd = WirelessConfig.TOTAL_SLOTS;
        if (slotId >= filterStart && slotId < filterEnd
                && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP)) {
            Slot slot = this.slots.get(slotId);
            ItemStack carried = this.getCarried();
            if (clickType == ClickType.QUICK_MOVE || carried.isEmpty() && clickType == ClickType.PICKUP) {
                slot.set(ItemStack.EMPTY);
            } else if (clickType == ClickType.SWAP) {
                ItemStack from = (button >= 0 && button < 9) ? p.getInventory().getItem(button) : ItemStack.EMPTY;
                setGhost(slot, from);
            } else {
                setGhost(slot, carried);
            }
            return;
        }
        super.clicked(slotId, button, clickType, p);
    }

    private static void setGhost(Slot slot, ItemStack source) {
        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
            return;
        }
        ItemStack ghost = source.copy();
        ghost.setCount(1);
        slot.set(ghost);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < WirelessConfig.UPGRADE_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (index >= PLAYER_START && stack.getItem() instanceof WirelessUpgradeItem) {
            if (!this.moveItemStackTo(stack, 0, WirelessConfig.UPGRADE_SLOTS, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return terminal().getItem() instanceof WirelessTerminalItem;
    }
}
