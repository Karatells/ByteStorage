package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.CellTier;
import com.simpleae.bytestorage.api.storage.IByteStorageCell;
import com.simpleae.bytestorage.api.storage.ICellInventory;
import com.simpleae.bytestorage.api.storage.IDriveHost;
import com.simpleae.bytestorage.menu.DriveMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity для блоку накопичувача дисків (Drive).
 * Вміщує до 10 дисків зберігання та підключає їхній об'єм до віртуального пулу мережі.
 */
public class DriveBlockEntity extends BlockEntity implements IDriveHost, MenuProvider {

    public static final int CELL_SLOTS = 10;
    public static final int FILTER_SLOTS = 9;
    public static final int MAX_PRIORITY = 999;

    private int priority = 0;
    /** Білий список (слоти-привиди). */
    private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private IGrid grid;
    private final ItemStackHandler driveInventory = new ItemStackHandler(CELL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof IByteStorageCell;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (grid != null) {
                // При зміні/вставці дисків сповіщаємо сховище мережі про оновлення кешу
                grid.getStorage().invalidateCache();
            }
        }
    };

    private final LazyOptional<IItemHandler> inventoryHandler = LazyOptional.of(() -> driveInventory);

    public DriveBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.DRIVE_BLOCK_ENTITY.get(), pos, state);
    }

    // --- IDriveHost ---

    @Override
    public List<ICellInventory> getActiveCellInventories() {
        List<ICellInventory> list = new ArrayList<>();
        for (int i = 0; i < driveInventory.getSlots(); i++) {
            ItemStack stack = driveInventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IByteStorageCell cellItem) {
                list.add(cellItem.getCellInventory(stack));
            }
        }
        return list;
    }

    @Override
    public IItemHandler getDriveInventory() {
        return driveInventory;
    }

    @Override
    public int getCellCount() {
        return CELL_SLOTS;
    }

    public ItemStackHandler getFilterInventory() {
        return filter;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void addPriority(int delta) {
        priority = Math.max(-MAX_PRIORITY, Math.min(MAX_PRIORITY, priority + delta));
        setChanged();
    }

    @Override
    public boolean acceptsItem(ItemStack stack) {
        boolean any = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack entry = filter.getStackInSlot(i);
            if (!entry.isEmpty()) {
                any = true;
                if (ItemStack.isSameItemSameTags(entry, stack)) {
                    return true;
                }
            }
        }
        return !any;
    }

    @Override
    public void onCellContentsChanged() {
        setChanged();
    }

    // --- IGridNode ---

    @Override
    @Nullable
    public IGrid getGrid() {
        return grid;
    }

    @Override
    public void setGrid(@Nullable IGrid grid) {
        this.grid = grid;
    }

    @Override
    public BlockPos getNodePos() {
        return this.worldPosition;
    }

    @Override
    public Level getNodeLevel() {
        return this.level;
    }

    @Override
    public int getIdleEnergyDrain() {
        int drain = 2; // Базове споживання Drive блоку (FE/t)
        for (int i = 0; i < driveInventory.getSlots(); i++) {
            ItemStack stack = driveInventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IByteStorageCell cellItem) {
                CellTier tier = cellItem.getTier(stack);
                drain += tier.getIdleEnergyDrain();
            }
        }
        return drain;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        // Оновлення стану відображення (світлодіоди дисків тощо)
        setChanged();
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.drive");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DriveMenu(containerId, playerInventory, this);
    }

    // --- Capabilities & Serialization ---

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("DriveInventory", driveInventory.serializeNBT());
        tag.put("Filter", filter.serializeNBT());
        tag.putInt("Priority", priority);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        priority = tag.getInt("Priority");
        if (tag.contains("Filter")) {
            filter.deserializeNBT(tag.getCompound("Filter"));
        }
        if (tag.contains("DriveInventory")) {
            driveInventory.deserializeNBT(tag.getCompound("DriveInventory"));
        }
    }
}
