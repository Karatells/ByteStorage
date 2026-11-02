package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.storage.IByteStorageCell;
import com.simpleae.bytestorage.block.entity.DriveBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
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
    public static final int FILTER_START = DRIVE_SLOTS;
    public static final int FILTER_END = FILTER_START + DriveBlockEntity.FILTER_SLOTS;
    public static final int PLAYER_START = FILTER_END;
    public static final int FILTER_X = 8;
    public static final int FILTER_Y = 73;
    public static final int PLAYER_INV_Y = 110;

    /** Кнопки меню: 0/1 — пріоритет +1/−1, 2/3 — +10/−10. */
    public static final int BUTTON_PRIORITY_UP = 0;
    public static final int BUTTON_PRIORITY_DOWN = 1;
    public static final int BUTTON_PRIORITY_UP_10 = 2;
    public static final int BUTTON_PRIORITY_DOWN_10 = 3;

    private final ContainerData data;
    private final DriveBlockEntity blockEntity;

    // Конструктор для клієнта (через мережевий буфер)
    public DriveMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, (DriveBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // Конструктор для сервера
    public DriveMenu(int containerId, Inventory playerInventory, DriveBlockEntity blockEntity) {
        super(SimpleByteStorage.DRIVE_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        this.data = playerInventory.player.level().isClientSide || blockEntity == null
                ? new SimpleContainerData(1)
                : new ContainerData() {
                    @Override
                    public int get(int index) {
                        return blockEntity.getPriority();
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

        // Білий список: 9 слотів-привидів
        IItemHandler filterInv = blockEntity != null ? blockEntity.getFilterInventory() : new ItemStackHandler(DriveBlockEntity.FILTER_SLOTS);
        for (int i = 0; i < DriveBlockEntity.FILTER_SLOTS; i++) {
            this.addSlot(new SlotItemHandler(filterInv, i, FILTER_X + i * 18, FILTER_Y) {
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

        // Інвентар гравця (3 рядки по 9 слотів)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        // Хотбар гравця (9 слотів)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public int getPriority() {
        return data.get(0);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= FILTER_START && slotId < FILTER_END
                && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP)) {
            Slot slot = this.slots.get(slotId);
            ItemStack source = ItemStack.EMPTY;
            if (clickType == ClickType.SWAP) {
                if (button >= 0 && button < 9) {
                    source = player.getInventory().getItem(button);
                }
            } else if (clickType == ClickType.PICKUP) {
                source = this.getCarried();
            }
            if (source.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                ItemStack ghost = source.copy();
                ghost.setCount(1);
                slot.set(ghost);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null || player.level().isClientSide) {
            return true;
        }
        switch (id) {
            case BUTTON_PRIORITY_UP -> blockEntity.addPriority(1);
            case BUTTON_PRIORITY_DOWN -> blockEntity.addPriority(-1);
            case BUTTON_PRIORITY_UP_10 -> blockEntity.addPriority(10);
            case BUTTON_PRIORITY_DOWN_10 -> blockEntity.addPriority(-10);
            default -> {
                return false;
            }
        }
        return true;
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

            if (index >= FILTER_START && index < FILTER_END) {
                return ItemStack.EMPTY;
            }
            if (index < DRIVE_SLOTS) {
                // Переміщення з Drive в інвентар гравця
                if (!this.moveItemStackTo(stackInSlot, PLAYER_START, this.slots.size(), true)) {
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
