package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.ArchiverBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Меню Байт-архіватора: перемикач увімкнення/паузи, статус мережі та інвентар гравця. */
public class ArchiverMenu extends AbstractContainerMenu {

    public static final int BUTTON_TOGGLE = 0;
    public static final int PLAYER_INV_Y = 88;

    private final ArchiverBlockEntity archiver;
    private final ContainerData data;

    public ArchiverMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (ArchiverBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ArchiverMenu(int containerId, Inventory playerInventory, ArchiverBlockEntity archiver) {
        super(SimpleByteStorage.ARCHIVER_MENU.get(), containerId);
        this.archiver = archiver;
        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(3)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return switch (index) {
                            case 0 -> archiver.isEnabled() ? 1 : 0;
                            case 1 -> archiver.getGrid() != null && archiver.getGrid().getState().isActive() ? 1 : 0;
                            default -> archiver.getGrid() != null && archiver.getGrid().isEcoMode() ? 1 : 0;
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

    public boolean isEnabled() {
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
        if (id == BUTTON_TOGGLE && !player.level().isClientSide && archiver != null) {
            archiver.toggleEnabled();
            return true;
        }
        return id == BUTTON_TOGGLE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (archiver == null) return false;
        Level level = archiver.getNodeLevel();
        return level != null
                && level.getBlockState(archiver.getNodePos()).is(SimpleByteStorage.ARCHIVER_BLOCK.get())
                && player.distanceToSqr(archiver.getNodePos().getX() + 0.5, archiver.getNodePos().getY() + 0.5,
                archiver.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
