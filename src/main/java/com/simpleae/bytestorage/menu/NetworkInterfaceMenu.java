package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.NetworkInterfaceBlockEntity;
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

/** Меню Сетевого интерфейса: 9 ghost-слотов конфигурации запаса + 9 слотов буфера + инвентарь игрока. */
public class NetworkInterfaceMenu extends AbstractContainerMenu {

    public static final int CONFIG_START = 0;
    public static final int CONFIG_END = NetworkInterfaceBlockEntity.SLOTS;
    public static final int BUFFER_START = CONFIG_END;
    public static final int BUFFER_END = BUFFER_START + NetworkInterfaceBlockEntity.SLOTS;
    public static final int PLAYER_START = BUFFER_END;
    public static final int PLAYER_END = PLAYER_START + 36;
    public static final int PLAYER_INV_Y = 98;

    private final NetworkInterfaceBlockEntity iface;
    private final ContainerData data;

    public NetworkInterfaceMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (NetworkInterfaceBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public NetworkInterfaceMenu(int containerId, Inventory playerInventory, NetworkInterfaceBlockEntity iface) {
        super(SimpleByteStorage.NETWORK_INTERFACE_MENU.get(), containerId);
        this.iface = iface;

        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(1)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return iface != null ? iface.getStatusCode() : 0;
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

        for (int i = 0; i < NetworkInterfaceBlockEntity.SLOTS; i++) {
            this.addSlot(new SlotItemHandler(iface.getConfigInventory(), i, 8 + i * 18, 30) {
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
        for (int i = 0; i < NetworkInterfaceBlockEntity.SLOTS; i++) {
            this.addSlot(new SlotItemHandler(iface.getBufferInventory(), i, 8 + i * 18, 62));
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

    public int getStatusCode() {
        return data.get(0);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= CONFIG_START && slotId < CONFIG_END
                && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP)) {
            Slot slot = this.slots.get(slotId);
            ItemStack carried = this.getCarried();
            if (clickType == ClickType.QUICK_MOVE) {
                slot.set(ItemStack.EMPTY);
            } else if (!carried.isEmpty()) {
                ItemStack ghost = carried.copy();
                if (button == 1) {
                    ghost.setCount(1);
                }
                slot.set(ghost);
            } else if (slot.hasItem()) {
                if (button == 1) {
                    ItemStack cur = slot.getItem().copy();
                    cur.shrink(1);
                    slot.set(cur.isEmpty() ? ItemStack.EMPTY : cur);
                } else {
                    slot.set(ItemStack.EMPTY);
                }
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem() || (index >= CONFIG_START && index < CONFIG_END)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index >= BUFFER_START && index < BUFFER_END) {
            if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, BUFFER_START, BUFFER_END, false)) {
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
        if (iface == null) {
            return false;
        }
        Level level = iface.getLevel();
        return level != null
                && level.getBlockState(iface.getBlockPos()).is(SimpleByteStorage.NETWORK_INTERFACE_BLOCK.get())
                && player.distanceToSqr(iface.getBlockPos().getX() + 0.5, iface.getBlockPos().getY() + 0.5,
                iface.getBlockPos().getZ() + 0.5) <= 64.0;
    }
}
