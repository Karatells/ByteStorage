package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.CellWorkbenchBlockEntity;
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

/** Меню Верстака ячеек: 1 слот ячейки, 9 слотів фільтра, перемикач режиму і очищення. */
public class CellWorkbenchMenu extends AbstractContainerMenu {

    public static final int BUTTON_BLACKLIST = 0;
    public static final int BUTTON_CLEAR = 1;

    public static final int CELL_SLOT = 0;
    public static final int FILTER_START = 1;
    public static final int FILTER_END = FILTER_START + CellWorkbenchBlockEntity.FILTER_SLOTS;
    public static final int PLAYER_START = FILTER_END;
    public static final int PLAYER_END = PLAYER_START + 36;
    public static final int PLAYER_INV_Y = 98;

    private final CellWorkbenchBlockEntity wb;
    private final ContainerData data;

    public CellWorkbenchMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (CellWorkbenchBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CellWorkbenchMenu(int containerId, Inventory playerInventory, CellWorkbenchBlockEntity wb) {
        super(SimpleByteStorage.CELL_WORKBENCH_MENU.get(), containerId);
        this.wb = wb;

        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(1)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return wb.isBlacklistMode() ? 1 : 0;
                    }

                    @Override
                    public void set(int index, int value) {
                    }

                    @Override
                    public int getCount() {
                        return 1;
                    }
                };
        this.addDataSlots(this.data);

        this.addSlot(new SlotItemHandler(wb.getCellInventory(), 0, 12, 24));
        for (int i = 0; i < CellWorkbenchBlockEntity.FILTER_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(wb.getFilterInventory(), i, 8 + i * 18, 52) {
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

    public boolean isBlacklistMode() {
        return data.get(0) != 0;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= FILTER_START && slotId < FILTER_END
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
        if (!player.level().isClientSide && wb != null) {
            if (id == BUTTON_BLACKLIST) {
                wb.toggleBlacklistMode();
                return true;
            }
            if (id == BUTTON_CLEAR) {
                wb.clearFilter();
                return true;
            }
        }
        return id == BUTTON_BLACKLIST || id == BUTTON_CLEAR;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem() || (index >= FILTER_START && index < FILTER_END)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == CELL_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, CELL_SLOT, CELL_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (wb == null) {
            return false;
        }
        Level level = wb.getLevel();
        return level != null
                && level.getBlockState(wb.getBlockPos()).is(SimpleByteStorage.CELL_WORKBENCH_BLOCK.get())
                && player.distanceToSqr(wb.getBlockPos().getX() + 0.5, wb.getBlockPos().getY() + 0.5,
                wb.getBlockPos().getZ() + 0.5) <= 64.0;
    }
}
