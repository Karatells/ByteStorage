package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.ICellInventory;
import com.simpleae.bytestorage.api.storage.IDriveHost;
import com.simpleae.bytestorage.menu.StorageBusMenu;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Шина зберігання (Storage Bus): підключає сусідні інвентарі до мережі як віртуальні диски
 * без перенесення предметів у байт-ячейки. Підтримує пріоритет і білий/чорний фільтр.
 */
public class StorageBusBlockEntity extends BlockEntity implements IGridNode, IDriveHost, MenuProvider {

    public static final int FILTER_SLOTS = 9;
    public static final int MAX_PRIORITY = 999;

    private IGrid grid;
    private int priority = 0;
    private boolean blacklistMode = false;
    private int containerCount = 0;
    private int tickCounter = 0;
    private int lastObservedHash = 0;

    private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler emptyDrive = new ItemStackHandler(0);

    public StorageBusBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.STORAGE_BUS_BLOCK_ENTITY.get(), pos, state);
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

    public boolean isBlacklistMode() {
        return blacklistMode;
    }

    public void toggleBlacklistMode() {
        blacklistMode = !blacklistMode;
        setChanged();
    }

    public int getContainerCount() {
        return containerCount;
    }

    public boolean isOnline() {
        return grid != null && grid.getState().isActive();
    }

    // --- IDriveHost ---

    @Override
    public List<ICellInventory> getActiveCellInventories() {
        List<ICellInventory> result = new ArrayList<>();
        if (level == null || level.isClientSide) {
            return result;
        }
        for (Direction dir : Direction.values()) {
            BlockPos adj = worldPosition.relative(dir);
            if (!level.isLoaded(adj)) continue;
            BlockEntity be = level.getBlockEntity(adj);
            if (be != null && !(be instanceof IGridNode)) {
                be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite())
                        .ifPresent(handler -> result.add(new ExternalCellInventory(handler)));
            }
        }
        return result;
    }

    @Override
    public IItemHandler getDriveInventory() {
        return emptyDrive;
    }

    @Override
    public int getCellCount() {
        return containerCount;
    }

    @Override
    public boolean acceptsItem(ItemStack stack) {
        boolean any = false;
        boolean matched = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack entry = filter.getStackInSlot(i);
            if (!entry.isEmpty()) {
                any = true;
                if (ItemStack.isSameItemSameTags(entry, stack)) {
                    matched = true;
                    break;
                }
            }
        }
        if (!any) {
            return true;
        }
        return blacklistMode != matched;
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
        return 2;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    // --- Тік ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, StorageBusBlockEntity bus) {
        if (++bus.tickCounter % 20 != 0) {
            return;
        }
        List<ICellInventory> cells = bus.getActiveCellInventories();
        bus.containerCount = cells.size();
        int hash = cells.size();
        for (ICellInventory cell : cells) {
            hash = 31 * hash + cell.getUsedBytes();
        }
        if (hash != bus.lastObservedHash) {
            bus.lastObservedHash = hash;
            if (bus.grid != null && bus.grid.getStorage() != null) {
                bus.grid.getStorage().invalidateCache();
            }
        }
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.storage_bus");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageBusMenu(containerId, playerInventory, this);
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Filter", filter.serializeNBT());
        tag.putInt("Priority", priority);
        tag.putBoolean("Blacklist", blacklistMode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Filter")) {
            filter.deserializeNBT(tag.getCompound("Filter"));
        }
        priority = tag.getInt("Priority");
        blacklistMode = tag.getBoolean("Blacklist");
    }

    /** Обгортка IItemHandler зовнішнього контейнера під інтерфейс ICellInventory. */
    private static final class ExternalCellInventory implements ICellInventory {

        private final IItemHandler handler;

        ExternalCellInventory(IItemHandler handler) {
            this.handler = handler;
        }

        @Override
        public int getMaxBytes() {
            long total = 0;
            for (int i = 0; i < handler.getSlots(); i++) {
                total += handler.getSlotLimit(i);
            }
            return (int) Math.min(Integer.MAX_VALUE, total);
        }

        @Override
        public int getUsedBytes() {
            long total = 0;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack s = handler.getStackInSlot(i);
                if (!s.isEmpty()) {
                    total += s.getCount();
                }
            }
            return (int) Math.min(Integer.MAX_VALUE, total);
        }

        @Override
        public List<ItemStack> getStoredItems() {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack s = handler.getStackInSlot(i);
                if (!s.isEmpty()) {
                    items.add(s);
                }
            }
            return items;
        }

        @Override
        public ItemStack insertItem(ItemStack toInsert, boolean simulate) {
            if (toInsert.isEmpty()) return ItemStack.EMPTY;
            return ItemHandlerHelper.insertItemStacked(handler, toInsert.copy(), simulate);
        }

        @Override
        public ItemStack extractItem(ItemStack toExtract, int amount, boolean simulate) {
            if (toExtract.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            int remaining = amount;
            ItemStack result = ItemStack.EMPTY;
            for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
                ItemStack inSlot = handler.getStackInSlot(i);
                if (!inSlot.isEmpty() && ItemStack.isSameItemSameTags(inSlot, toExtract)) {
                    ItemStack taken = handler.extractItem(i, remaining, simulate);
                    if (!taken.isEmpty()) {
                        if (result.isEmpty()) {
                            result = taken.copy();
                        } else {
                            result.grow(taken.getCount());
                        }
                        remaining -= taken.getCount();
                    }
                }
            }
            return result;
        }

        @Override
        public void clear() {
            // Зовнішній контейнер не очищується примусово
        }
    }
}
