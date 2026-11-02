package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.PatternProviderBlockEntity;
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

/**
 * Меню Шини Шаблонів: 9 слотів для процесних шаблонів + інвентар гравця.
 * ContainerData: 0 — черга, 1 — кількість машин поруч, 2 — мережа активна.
 */
public class PatternProviderMenu extends AbstractContainerMenu {

    public static final int PATTERN_SLOTS = PatternProviderBlockEntity.SLOTS;
    public static final int PLAYER_START = PATTERN_SLOTS;
    public static final int PLAYER_END = PLAYER_START + 36;
    public static final int SLOTS_X = 8;
    public static final int SLOTS_Y = 24;
    public static final int PLAYER_INV_Y = 88;

    private final PatternProviderBlockEntity provider;
    private final ContainerData data;

    public PatternProviderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (PatternProviderBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public PatternProviderMenu(int containerId, Inventory playerInventory, PatternProviderBlockEntity provider) {
        super(SimpleByteStorage.PATTERN_PROVIDER_MENU.get(), containerId);
        this.provider = provider;

        this.data = playerInventory.player.level().isClientSide
                ? new SimpleContainerData(3)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return switch (index) {
                            case 0 -> provider.getPendingCrafts();
                            case 1 -> provider.getMachineCount();
                            default -> provider.isOnline() ? 1 : 0;
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

        for (int i = 0; i < PATTERN_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(provider.getInventoryHandler(), i, SLOTS_X + i * 18, SLOTS_Y));
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

    public int getQueue() {
        return data.get(0);
    }

    public int getMachines() {
        return data.get(1);
    }

    public boolean isOnline() {
        return data.get(2) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PATTERN_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, PATTERN_SLOTS, false)) {
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
        if (provider == null) {
            return false;
        }
        Level level = provider.getNodeLevel();
        return level != null
                && level.getBlockState(provider.getNodePos()).is(SimpleByteStorage.PATTERN_PROVIDER_BLOCK.get())
                && player.distanceToSqr(provider.getNodePos().getX() + 0.5, provider.getNodePos().getY() + 0.5,
                provider.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
