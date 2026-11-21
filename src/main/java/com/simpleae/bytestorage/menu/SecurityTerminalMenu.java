package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.SecurityTerminalBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Меню Термінала безпеки: стан блокування, власник і перемикач доступу. */
public class SecurityTerminalMenu extends AbstractContainerMenu {

    public static final int BUTTON_LOCK = 0;
    public static final int PLAYER_INV_Y = 88;

    private final SecurityTerminalBlockEntity sec;
    private final ContainerData data;
    private final String ownerName;

    public SecurityTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (SecurityTerminalBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                extraData.readUtf(64));
    }

    public SecurityTerminalMenu(int containerId, Inventory playerInventory, SecurityTerminalBlockEntity sec) {
        this(containerId, playerInventory, sec, sec != null ? sec.getOwnerName() : "");
    }

    private SecurityTerminalMenu(int containerId, Inventory playerInventory, SecurityTerminalBlockEntity sec, String ownerName) {
        super(SimpleByteStorage.SECURITY_TERMINAL_MENU.get(), containerId);
        this.sec = sec;
        this.ownerName = ownerName;
        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(2)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return switch (index) {
                            case 0 -> sec.isLocked() ? 1 : 0;
                            default -> sec.getGrid() != null && sec.getGrid().getState().isActive() ? 1 : 0;
                        };
                    }

                    @Override
                    public void set(int index, int value) {
                    }

                    @Override
                    public int getCount() {
                        return 2;
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

    public boolean isLocked() {
        return data.get(0) != 0;
    }

    public boolean isOnline() {
        return data.get(1) != 0;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_LOCK && !player.level().isClientSide && sec != null && sec.isOwner(player)) {
            sec.toggleLocked();
            return true;
        }
        return id == BUTTON_LOCK;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (sec == null) return false;
        Level level = sec.getNodeLevel();
        return level != null
                && level.getBlockState(sec.getNodePos()).is(SimpleByteStorage.SECURITY_TERMINAL_BLOCK.get())
                && player.distanceToSqr(sec.getNodePos().getX() + 0.5, sec.getNodePos().getY() + 0.5,
                sec.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
