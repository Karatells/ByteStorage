package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.StorageBusBlockEntity;
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

/** Меню Шини зберігання: 9 слотів фільтра, пріоритет, білий/чорний список. */
public class StorageBusMenu extends AbstractContainerMenu {

    public static final int BUTTON_PRIO_DOWN = 0;
    public static final int BUTTON_PRIO_UP = 1;
    public static final int BUTTON_BLACKLIST = 2;

    public static final int FILTER_SLOTS = StorageBusBlockEntity.FILTER_SLOTS;
    public static final int PLAYER_START = FILTER_SLOTS;
    public static final int PLAYER_END = PLAYER_START + 36;
    public static final int FILTER_X = 8;
    public static final int FILTER_Y = 24;
    public static final int PLAYER_INV_Y = 88;

    private final StorageBusBlockEntity bus;
    private final ContainerData data;

    public StorageBusMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (StorageBusBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public StorageBusMenu(int containerId, Inventory playerInventory, StorageBusBlockEntity bus) {
        super(SimpleByteStorage.STORAGE_BUS_MENU.get(), containerId);
        this.bus = bus;

        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(4)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return switch (index) {
                            case 0 -> bus.getPriority();
                            case 1 -> bus.getContainerCount();
                            case 2 -> bus.isOnline() ? 1 : 0;
                            default -> bus.isBlacklistMode() ? 1 : 0;
                        };
                    }

                    @Override
                    public void set(int index, int value) {
                    }

                    @Override
                    public int getCount() {
                        return 4;
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

    public int getPriority() {
        return data.get(0);
    }

    public int getContainers() {
        return data.get(1);
    }

    public boolean isOnline() {
        return data.get(2) != 0;
    }

    public boolean isBlacklistMode() {
        return data.get(3) != 0;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < FILTER_SLOTS
                && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP)) {
            Slot slot = this.slots.get(slotId);
            ItemStack carried = this.getCarried();
            if (clickType == ClickType.QUICK_MOVE || carried.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                ItemStack ghost = carried.copy();
                ghost.setCount(1);
                slot.set(ghost);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide && bus != null) {
            switch (id) {
                case BUTTON_PRIO_DOWN -> {
                    bus.addPriority(-1);
                    return true;
                }
                case BUTTON_PRIO_UP -> {
                    bus.addPriority(1);
                    return true;
                }
                case BUTTON_BLACKLIST -> {
                    bus.toggleBlacklistMode();
                    return true;
                }
                default -> {
                }
            }
        }
        return id >= BUTTON_PRIO_DOWN && id <= BUTTON_BLACKLIST;
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
                && level.getBlockState(bus.getNodePos()).is(SimpleByteStorage.STORAGE_BUS_BLOCK.get())
                && player.distanceToSqr(bus.getNodePos().getX() + 0.5, bus.getNodePos().getY() + 0.5,
                bus.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
