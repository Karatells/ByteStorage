package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.storage.IByteStorageCell;
import com.simpleae.bytestorage.block.entity.AssemblerBlockEntity;
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
import org.jetbrains.annotations.NotNull;

/**
 * Меню Асемблера: слот карточки пам'яті + інвентар гравця (3×9 + хотбар).
 * Прогрес, фаза, довжина черги та причина очікування приходять через ContainerData.
 *
 * Кнопка меню 0 — скасувати всі замовлення (вміст карточки повертається в мережу).
 */
public class AssemblerMenu extends AbstractContainerMenu {

    public static final int BUTTON_CANCEL = 0;

    public static final int SLOT_CARD = 0;
    public static final int PLAYER_START = 1;
    public static final int PLAYER_END = PLAYER_START + 36;

    public static final int CARD_X = 15;
    public static final int CARD_Y = 27;
    public static final int PLAYER_INV_Y = 136;

    private final AssemblerBlockEntity assembler;
    private final ContainerData data;

    public AssemblerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                (AssemblerBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(AssemblerBlockEntity.DATA_COUNT));
    }

    public AssemblerMenu(int containerId, Inventory playerInventory, AssemblerBlockEntity assembler, ContainerData data) {
        super(SimpleByteStorage.ASSEMBLER_MENU.get(), containerId);
        this.assembler = assembler;
        this.data = data;

        this.addSlot(new SlotItemHandler(assembler.getInventoryHandler(), AssemblerBlockEntity.BUFFER_SLOT, CARD_X, CARD_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return !isBusy() && stack.getItem() instanceof IByteStorageCell;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !isBusy();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }

        this.addDataSlots(this.data);
    }

    // --- Дані для GUI ---

    public int getProgress() {
        return data.get(AssemblerBlockEntity.DATA_PROGRESS);
    }

    public int getProgressMax() {
        return Math.max(1, data.get(AssemblerBlockEntity.DATA_PROGRESS_MAX));
    }

    public int getPhase() {
        return data.get(AssemblerBlockEntity.DATA_PHASE);
    }

    public boolean isBusy() {
        return getPhase() != AssemblerBlockEntity.PHASE_IDLE;
    }

    public int getQueue() {
        return data.get(AssemblerBlockEntity.DATA_QUEUE);
    }

    public int getWaitReason() {
        return data.get(AssemblerBlockEntity.DATA_WAIT);
    }

    public ItemStack getCurrentOutput() {
        return assembler != null ? assembler.getCurrentOutput() : ItemStack.EMPTY;
    }

    public ItemStack getCard() {
        return this.slots.get(SLOT_CARD).getItem();
    }

    // --- Кнопки ---

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CANCEL) {
            if (!player.level().isClientSide && assembler != null) {
                assembler.cancelJobs();
            }
            return true;
        }
        return false;
    }

    // --- Shift-клік ---

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stackInSlot = slot.getItem();
        ItemStack original = stackInSlot.copy();

        if (index == SLOT_CARD) {
            if (!this.moveItemStackTo(stackInSlot, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stackInSlot.getItem() instanceof IByteStorageCell) {
            if (!this.moveItemStackTo(stackInSlot, SLOT_CARD, SLOT_CARD + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (assembler == null) {
            return false;
        }
        Level level = assembler.getNodeLevel();
        return level != null
                && level.getBlockState(assembler.getNodePos()).is(SimpleByteStorage.ASSEMBLER_BLOCK.get())
                && player.distanceToSqr(assembler.getNodePos().getX() + 0.5, assembler.getNodePos().getY() + 0.5,
                assembler.getNodePos().getZ() + 0.5) <= 64.0;
    }
}
