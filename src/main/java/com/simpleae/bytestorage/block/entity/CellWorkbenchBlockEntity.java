package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.storage.IByteStorageCell;
import com.simpleae.bytestorage.menu.CellWorkbenchMenu;
import com.simpleae.bytestorage.storage.ByteCellInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** BlockEntity Верстака ячеек: зберігає фільтр безпосередньо в NBT вставленої байт-ячейки. */
public class CellWorkbenchBlockEntity extends BlockEntity implements MenuProvider {

    public static final int FILTER_SLOTS = 9;

    private boolean syncing = false;
    private boolean blacklistMode = false;

    private final ItemStackHandler cellSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof IByteStorageCell
                    || stack.getItem() instanceof com.simpleae.bytestorage.item.FluidCellItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            loadFromCell();
            setChanged();
        }
    };

    private final ItemStackHandler targetCellSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof IByteStorageCell
                    || stack.getItem() instanceof com.simpleae.bytestorage.item.FluidCellItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            saveToCell();
            setChanged();
        }
    };

    public CellWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.CELL_WORKBENCH_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getCellInventory() {
        return cellSlot;
    }

    public ItemStackHandler getTargetCellInventory() {
        return targetCellSlot;
    }

    public ItemStackHandler getFilterInventory() {
        return filter;
    }

    /** Переносит содержимое из исходной ячейки (слот 0) в целевую ячейку (слот 1) с учётом фильтров и ёмкости. */
    public boolean migrateCells() {
        ItemStack src = cellSlot.getStackInSlot(0);
        ItemStack dst = targetCellSlot.getStackInSlot(0);
        if (src.isEmpty() || dst.isEmpty()) {
            return false;
        }
        boolean movedAny = false;
        if (src.getItem() instanceof IByteStorageCell srcCell && dst.getItem() instanceof IByteStorageCell dstCell) {
            com.simpleae.bytestorage.api.storage.ICellInventory srcInv = srcCell.getCellInventory(src);
            com.simpleae.bytestorage.api.storage.ICellInventory dstInv = dstCell.getCellInventory(dst);
            if (srcInv == null || dstInv == null) {
                return false;
            }
            java.util.List<ItemStack> snapshot = new java.util.ArrayList<>(srcInv.getStoredItems());
            for (ItemStack stack : snapshot) {
                if (stack.isEmpty()) continue;
                ItemStack simRem = dstInv.insertItem(stack.copy(), true);
                int fits = stack.getCount() - simRem.getCount();
                if (fits <= 0) continue;
                ItemStack extracted = srcInv.extractItem(stack, fits, false);
                if (!extracted.isEmpty()) {
                    ItemStack rem = dstInv.insertItem(extracted, false);
                    if (!rem.isEmpty()) {
                        srcInv.insertItem(rem, false);
                    }
                    movedAny = true;
                }
            }
            if (movedAny) {
                srcInv.flush();
                dstInv.flush();
                setChanged();
            }
            return movedAny;
        }
        if (src.getItem() instanceof com.simpleae.bytestorage.item.FluidCellItem
                && dst.getItem() instanceof com.simpleae.bytestorage.item.FluidCellItem dstFluid) {
            java.util.List<net.minecraftforge.fluids.FluidStack> fluids =
                    com.simpleae.bytestorage.item.FluidCellItem.getStoredFluids(src);
            java.util.List<net.minecraftforge.fluids.FluidStack> remaining = new java.util.ArrayList<>();
            for (net.minecraftforge.fluids.FluidStack fs : fluids) {
                if (fs.isEmpty()) continue;
                int inserted = dstFluid.insertFluid(dst, fs, false);
                if (inserted > 0) {
                    movedAny = true;
                }
                int left = fs.getAmount() - inserted;
                if (left > 0) {
                    net.minecraftforge.fluids.FluidStack copy = fs.copy();
                    copy.setAmount(left);
                    remaining.add(copy);
                }
            }
            if (movedAny) {
                com.simpleae.bytestorage.item.FluidCellItem.saveFluids(src, remaining);
                setChanged();
            }
            return movedAny;
        }
        return false;
    }

    public boolean isBlacklistMode() {
        return blacklistMode;
    }

    public void toggleBlacklistMode() {
        blacklistMode = !blacklistMode;
        saveToCell();
        setChanged();
    }

    public void clearFilter() {
        syncing = true;
        try {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                filter.setStackInSlot(i, ItemStack.EMPTY);
            }
            blacklistMode = false;
        } finally {
            syncing = false;
        }
        saveToCell();
        setChanged();
    }

    private void loadFromCell() {
        syncing = true;
        try {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                filter.setStackInSlot(i, ItemStack.EMPTY);
            }
            blacklistMode = false;
            ItemStack cell = cellSlot.getStackInSlot(0);
            if (cell.isEmpty() || !cell.hasTag()) {
                return;
            }
            CompoundTag tag = cell.getTag();
            blacklistMode = tag.getBoolean(ByteCellInventory.TAG_CELL_BLACKLIST);
            if (tag.contains(ByteCellInventory.TAG_CELL_FILTER, Tag.TAG_LIST)) {
                ListTag list = tag.getList(ByteCellInventory.TAG_CELL_FILTER, Tag.TAG_COMPOUND);
                for (int i = 0; i < Math.min(FILTER_SLOTS, list.size()); i++) {
                    filter.setStackInSlot(i, ItemStack.of(list.getCompound(i)));
                }
            }
        } finally {
            syncing = false;
        }
    }

    private void saveToCell() {
        if (syncing) return;
        ItemStack cell = cellSlot.getStackInSlot(0);
        if (cell.isEmpty()) return;
        CompoundTag tag = cell.getOrCreateTag();
        ListTag list = new ListTag();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack s = filter.getStackInSlot(i);
            if (!s.isEmpty()) {
                ItemStack one = s.copy();
                one.setCount(1);
                list.add(one.save(new CompoundTag()));
            }
        }
        if (list.isEmpty()) {
            tag.remove(ByteCellInventory.TAG_CELL_FILTER);
            tag.remove(ByteCellInventory.TAG_CELL_BLACKLIST);
        } else {
            tag.put(ByteCellInventory.TAG_CELL_FILTER, list);
            tag.putBoolean(ByteCellInventory.TAG_CELL_BLACKLIST, blacklistMode);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.cell_workbench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CellWorkbenchMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Cell", cellSlot.serializeNBT());
        tag.put("TargetCell", targetCellSlot.serializeNBT());
        tag.put("Filter", filter.serializeNBT());
        tag.putBoolean("Blacklist", blacklistMode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Cell")) {
            cellSlot.deserializeNBT(tag.getCompound("Cell"));
        }
        if (tag.contains("TargetCell")) {
            targetCellSlot.deserializeNBT(tag.getCompound("TargetCell"));
        }
        if (tag.contains("Filter")) {
            filter.deserializeNBT(tag.getCompound("Filter"));
        }
        blacklistMode = tag.getBoolean("Blacklist");
    }
}
