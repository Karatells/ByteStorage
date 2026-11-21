package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.FluidIoBusBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Меню Рідинної I/O-шини: перемикання Імпорт/Експорт та стан мережі. */
public class FluidIoBusMenu extends AbstractContainerMenu {

    public static final int BUTTON_MODE = 0;
    public static final int PLAYER_INV_Y = 88;

    private final FluidIoBusBlockEntity bus;
    private final ContainerData data;

    public FluidIoBusMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (FluidIoBusBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public FluidIoBusMenu(int containerId, Inventory playerInventory, FluidIoBusBlockEntity bus) {
        super(SimpleByteStorage.FLUID_IO_BUS_MENU.get(), containerId);
        this.bus = bus;
        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(3)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return switch (index) {
                            case 0 -> bus.isExportMode() ? 1 : 0;
                            case 1 -> bus.getGrid() != null && bus.getGrid().getState().isActive() ? 1 : 0;
                            default -> bus.getGrid() != null && bus.getGrid().isEcoMode() ? 1 : 0;
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

    public boolean isOnline() {
        return data.get(1) != 0;
    }

    public boolean isEcoMode() {
        return data.get(2) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_MODE && !player.level().isClientSide && bus != null) {
            bus.toggleExportMode();
            return true;
        }
        return id == BUTTON_MODE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (bus == null) return false;
        Level level = bus.getNodeLevel();
        return level != null
                && level.getBlockState(bus.getNodePos()).is(SimpleByteStorage.FLUID_IO_BUS_BLOCK.get())
                && player.distanceToSqr(bus.getNodePos().getX() + 0.5, bus.getNodePos().getY() + 0.5,
                bus.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
