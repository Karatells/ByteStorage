package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.QuantumRelayBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;

/** Меню Квантового ретранслятора: 1 слот для Запутаного кристала, кількість партнерів та статус. */
public class QuantumRelayMenu extends AbstractContainerMenu {

    public static final int CRYSTAL_SLOT = 0;
    public static final int PLAYER_START = 1;
    public static final int PLAYER_END = PLAYER_START + 36;
    public static final int PLAYER_INV_Y = 88;

    private final QuantumRelayBlockEntity relay;
    private final ContainerData data;

    public QuantumRelayMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (QuantumRelayBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public QuantumRelayMenu(int containerId, Inventory playerInventory, QuantumRelayBlockEntity relay) {
        super(SimpleByteStorage.QUANTUM_RELAY_MENU.get(), containerId);
        this.relay = relay;
        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(2)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return switch (index) {
                            case 0 -> relay.getGrid() != null && relay.getGrid().getState().isActive() ? 1 : 0;
                            default -> relay.getLinkedPartners().size();
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

        this.addSlot(new SlotItemHandler(relay.getCrystalInventory(), 0, 14, 29));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public boolean isOnline() {
        return data.get(0) != 0;
    }

    public int getPartnerCount() {
        return data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == CRYSTAL_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, CRYSTAL_SLOT, CRYSTAL_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (relay == null) return false;
        Level level = relay.getNodeLevel();
        return level != null
                && level.getBlockState(relay.getNodePos()).is(SimpleByteStorage.QUANTUM_RELAY_BLOCK.get())
                && player.distanceToSqr(relay.getNodePos().getX() + 0.5, relay.getNodePos().getY() + 0.5,
                relay.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
