package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.NetworkDisplayBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Меню Інформаційного монітора: відображає 6 рядків тексту зі скриптів та кнопку очищення. */
public class NetworkDisplayMenu extends AbstractContainerMenu {

    public static final int BUTTON_CLEAR = 0;
    public static final int PLAYER_INV_Y = 122;

    private final NetworkDisplayBlockEntity display;
    private final ContainerData data;
    private final String[] lines = new String[NetworkDisplayBlockEntity.MAX_LINES];

    public NetworkDisplayMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (NetworkDisplayBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                readLines(extraData));
    }

    public NetworkDisplayMenu(int containerId, Inventory playerInventory, NetworkDisplayBlockEntity display) {
        this(containerId, playerInventory, display, copyLines(display));
    }

    private NetworkDisplayMenu(int containerId, Inventory playerInventory, NetworkDisplayBlockEntity display, String[] initialLines) {
        super(SimpleByteStorage.NETWORK_DISPLAY_MENU.get(), containerId);
        this.display = display;
        System.arraycopy(initialLines, 0, this.lines, 0, NetworkDisplayBlockEntity.MAX_LINES);
        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(1)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return display.getGrid() != null && display.getGrid().getState().isActive() ? 1 : 0;
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

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }
    }

    private static String[] copyLines(NetworkDisplayBlockEntity display) {
        String[] arr = new String[NetworkDisplayBlockEntity.MAX_LINES];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = display != null ? display.getLine(i) : "";
        }
        return arr;
    }

    private static String[] readLines(FriendlyByteBuf buf) {
        String[] arr = new String[NetworkDisplayBlockEntity.MAX_LINES];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = buf.readUtf(128);
        }
        return arr;
    }

    public boolean isOnline() {
        return data.get(0) != 0;
    }

    public String getLine(int idx) {
        return idx >= 0 && idx < lines.length ? lines[idx] : "";
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CLEAR && !player.level().isClientSide && display != null) {
            display.clearLines();
            return true;
        }
        return id == BUTTON_CLEAR;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (display == null) return false;
        Level level = display.getNodeLevel();
        return level != null
                && level.getBlockState(display.getNodePos()).is(SimpleByteStorage.NETWORK_DISPLAY_BLOCK.get())
                && player.distanceToSqr(display.getNodePos().getX() + 0.5, display.getNodePos().getY() + 0.5,
                display.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
