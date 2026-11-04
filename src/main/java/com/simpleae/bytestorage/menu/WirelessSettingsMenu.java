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
 * Настройки Беспроводного терминала. Три вкладки: улучшения (8 слотов), перенос (режимы + фильтр на 18 слотов)
 * и макрос. Кнопки: 0 — автопополнение, 1 — автосбор, 2 — белый/чёрный список.
 * Терминал берётся из слота инвентаря игрока (индекс передаётся при открытии); этот слот заблокирован.
 */
public class WirelessSettingsMenu extends AbstractContainerMenu {

    public static final int TAB_UPGRADES = 0;
    public static final int TAB_TRANSFER = 1;
    public static final int TAB_MACRO = 2;

    public static final int UPGRADE_X = 8;
    public static final int UPGRADE_Y = 42;
    public static final int FILTER_X = 8;
    public static final int FILTER_Y = 72;
    public static final int PLAYER_INV_Y = 124;
    public static final int PLAYER_START = WirelessConfig.TOTAL_SLOTS;

    private final Player player;
    private final int terminalSlot;
    private final ItemStackHandler handler;
    private final ContainerData data;
    /** Активная вкладка (используется только клиентом: неактивные слоты скрываются и не кликаются). */
    private int tab = TAB_UPGRADES;

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
            final int index = i;
            this.addSlot(new SlotItemHandler(handler, i, UPGRADE_X + i * 18, UPGRADE_Y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof WirelessUpgradeItem u
                            && !WirelessConfig.hasUpgrade(handler, u.getType(), index);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return tab == TAB_UPGRADES;
                }
            });
        }
        for (int i = 0; i < WirelessConfig.FILTER_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(handler, WirelessConfig.UPGRADE_SLOTS + i,
                    FILTER_X + (i % 9) * 18, FILTER_Y + (i / 9) * 18) {
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
                    return tab == TAB_TRANSFER;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = col + row * 9 + 9;
                this.addSlot(new LockedSlot(inv, idx, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new LockedSlot(inv, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }
    }

    /** Слот инвентаря игрока; слот с самим терминалом заблокирован. */
    private class LockedSlot extends Slot {
        private final int idx;

        LockedSlot(Inventory inv, int idx, int x, int y) {
            super(inv, idx, x, y);
            this.idx = idx;
        }

        @Override
        public boolean mayPickup(Player p) {
            return idx != terminalSlot;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return idx != terminalSlot;
        }
    }

    public int getTab() {
        return tab;
    }

    public void setTab(int tab) {
        this.tab = tab;
    }

    private ItemStack terminal() {
        if (terminalSlot < 0 || terminalSlot >= player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(terminalSlot);
    }

    /** Клиент: терминал, открытый в настройках (для отображения названия макроса). */
    public ItemStack getTerminal() {
        return terminal();
    }

    public ItemStack getUpgradeStack(int index) {
        return handler.getStackInSlot(index);
    }

    public boolean hasUpgrade(WirelessUpgradeItem.Type type) {
        return WirelessConfig.hasUpgrade(handler, type, -1);
    }

    private void persist() {
        if (player.level().isClientSide) return;
        ItemStack t = terminal();
        if (t.getItem() instanceof WirelessTerminalItem) {
            WirelessConfig.save(t, handler);
        }
    }

    /** Сервер: сменить название макроса (приходит пакетом). */
    public void setMacro(String name) {
        ItemStack t = terminal();
        if (!player.level().isClientSide && t.getItem() instanceof WirelessTerminalItem) {
            WirelessConfig.setMacroName(t, name);
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
