package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.storage.IByteStorageCell;
import com.simpleae.bytestorage.block.entity.DriveBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Контейнер-меню для накопичувача дисків (Drive).
 * Дозволяє вставляти та витягувати до 10 дисків зберігання.
 */
public class DriveMenu extends AbstractContainerMenu {

    public static final int DRIVE_SLOTS = 10;
    private final DriveBlockEntity blockEntity;

    // Конструктор для клієнта (через мережевий буфер)
    public DriveMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, (DriveBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // Конструктор для сервера
    public DriveMenu(int containerId, Inventory playerInventory, DriveBlockEntity blockEntity) {
        super(SimpleByteStorage.DRIVE_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        IItemHandler driveInv = blockEntity != null ? blockEntity.getDriveInventory() : new ItemStackHandler(DRIVE_SLOTS);

        // Додаємо 10 слотів для дисків (2 стовпчики по 5 або 2 рядки по 5)
        // Координати інтерфейсу: 2 рядки по 5 слотів
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = col + row * 5;
                int x = 44 + col * 18;
                int y = 20 + row * 18;
                this.addSlot(new SlotItemHandler(driveInv, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return stack.getItem() instanceof IByteStorageCell;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1; // Диски не стакуються
                    }
                });
            }
        }

        // Інвентар гравця (3 рядки по 9 слотів)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 70 + row * 18));
            }
        }

        // Хотбар гравця (9 слотів)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 128));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return false;
        return stillValid(this.blockEntity.getNodeLevel(), this.blockEntity.getNodePos(), player, SimpleByteStorage.DRIVE_BLOCK.get());
    }

    private static boolean stillValid(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, Player player, net.minecraft.world.level.block.Block targetBlock) {
        return level != null && level.getBlockState(pos).is(targetBlock) && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < DRIVE_SLOTS) {
                // Переміщення з Drive в інвентар гравця
                if (!this.moveItemStackTo(stackInSlot, DRIVE_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Переміщення з інвентаря гравця в Drive (тільки якщо це ячейка!)
                if (stackInSlot.getItem() instanceof IByteStorageCell) {
                    if (!this.moveItemStackTo(stackInSlot, 0, DRIVE_SLOTS, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public DriveBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
