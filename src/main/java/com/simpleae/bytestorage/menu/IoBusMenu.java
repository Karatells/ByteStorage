package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.IoBusBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Меню I/O-шини: 9 слотів-привидів зі списком предметів (кількість = скільки тримати в режимі експорту)
 * та інвентар гравця. ContainerData: 0 — режим експорту, 1 — кількість машин поруч, 2 — мережа активна.
 * Кнопка 0 — перемкнути режим.
 */
public class IoBusMenu extends AbstractContainerMenu {

    public static final int BUTTON_MODE = 0;
    public static final int FILTER_SLOTS = IoBusBlockEntity.FILTER_SLOTS;
    public static final int PLAYER_START = FILTER_SLOTS;
    public static final int PLAYER_END = PLAYER_START + 36;
    public static final int FILTER_X = 8;
    public static final int FILTER_Y = 24;
    public static final int PLAYER_INV_Y = 88;

    private final IoBusBlockEntity bus;
    private final ContainerData data;

    public IoBusMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (IoBusBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public IoBusMenu(int containerId, Inventory playerInventory, IoBusBlockEntity bus) {
        super(SimpleByteStorage.IO_BUS_MENU.get(), containerId);
        this.bus = bus;

        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(3)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return switch (index) {
                            case 0 -> bus.isExportMode() ? 1 : 0;
                            case 1 -> bus.getMachineCount();
                            default -> bus.isOnline() ? 1 : 0;
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

        for (int i = 0; i < FILTER_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(bus.getFilterInventory(), i, FILTER_X + i * 18, FILTER_Y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public boolean isExportMode() {
        return data.get(0) != 0;
    }

    public int getMachines() {
        return data.get(1);
    }

    public boolean isOnline() {
        return data.get(2) != 0;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < FILTER_SLOTS
                && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP)) {
            Slot slot = this.slots.get(slotId);
            ItemStack carried = this.getCarried();
            ItemStack current = slot.getItem();
            if (clickType == ClickType.QUICK_MOVE) {
                slot.set(ItemStack.EMPTY);
            } else if (clickType == ClickType.SWAP) {
                ItemStack from = (button >= 0 && button < 9) ? player.getInventory().getItem(button) : ItemStack.EMPTY;
                setGhost(slot, from, from.getCount());
            } else if (carried.isEmpty()) {
                if (button == 1 && current.getCount() > 1) {
                    ItemStack less = current.copy();
                    less.shrink(1);
                    slot.set(less);
                } else {
                    slot.set(ItemStack.EMPTY);
                }
            } else if (button == 1) {
                int count = !current.isEmpty() && ItemStack.isSameItemSameTags(current, carried) ? current.getCount() + 1 : 1;
                setGhost(slot, carried, count);
            } else {
                setGhost(slot, carried, carried.getCount());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private static void setGhost(Slot slot, ItemStack source, int count) {
        if (source.isEmpty() || count <= 0) {
            slot.set(ItemStack.EMPTY);
            return;
        }
        ItemStack ghost = source.copy();
        ghost.setCount(Math.min(count, 64));
        slot.set(ghost);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_MODE) {
            if (!player.level().isClientSide && bus != null) {
                bus.toggleMode();
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (bus == null) {
            return false;
        }
        Level level = bus.getNodeLevel();
        return level != null
                && level.getBlockState(bus.getNodePos()).is(SimpleByteStorage.IO_BUS_BLOCK.get())
                && player.distanceToSqr(bus.getNodePos().getX() + 0.5, bus.getNodePos().getY() + 0.5,
                bus.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
