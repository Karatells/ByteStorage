package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.menu.IoBusMenu;
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
 * I/O-шина. Режим «Імпорт»: забирає предмети (за білим списком, порожній = усе) із сусіднього
 * інвентаря в мережу. Режим «Експорт»: підтримує в сусідньому інвентарі кількість предметів зі списку
 * (кількість у слоті-привиді = скільки тримати), добираючи з мережі.
 */
public class IoBusBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    public static final int FILTER_SLOTS = 9;
    public static final int UPGRADE_SLOTS = 4;
    public static final int REDSTONE_ALWAYS = 0;
    public static final int REDSTONE_HIGH = 1;
    public static final int REDSTONE_LOW = 2;
    private static final int CYCLE_TICKS = 10;
    private static final int ITEMS_PER_CYCLE = 64;

    private IGrid grid;
    private int tickCounter = 0;
    private int machineCount = 0;
    private boolean exportMode = false;
    private boolean blacklistMode = false;
    private int redstoneMode = REDSTONE_ALWAYS;

    private final ItemStackHandler upgrades = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof com.simpleae.bytestorage.item.SpeedCardItem;
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
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public IoBusBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.IO_BUS_BLOCK_ENTITY.get(), pos, state);
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

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.io_bus");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IoBusMenu(containerId, playerInventory, this);
    }

    // --- Стан ---

    public ItemStackHandler getFilterInventory() {
        return filter;
    }

    public ItemStackHandler getUpgradesInventory() {
        return upgrades;
    }

    public boolean isExportMode() {
        return exportMode;
    }

    public void toggleMode() {
        exportMode = !exportMode;
        setChanged();
    }

    public boolean isBlacklistMode() {
        return blacklistMode;
    }

    public void toggleBlacklistMode() {
        blacklistMode = !blacklistMode;
        setChanged();
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void cycleRedstoneMode() {
        redstoneMode = (redstoneMode + 1) % 3;
        setChanged();
    }

    public int getMachineCount() {
        return machineCount;
    }

    public boolean isOnline() {
        return grid != null && grid.getState().isActive();
    }

    public List<ItemStack> getDroppedItems() {
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            ItemStack s = upgrades.getStackInSlot(i);
            if (!s.isEmpty()) {
                drops.add(s.copy());
            }
        }
        return drops;
    }

    private int speedCardCount() {
        int count = 0;
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (!upgrades.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private boolean isRedstoneAllowed() {
        if (level == null || redstoneMode == REDSTONE_ALWAYS) {
            return true;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        return redstoneMode == REDSTONE_HIGH ? powered : !powered;
    }

    // --- Тік ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, IoBusBlockEntity bus) {
        bus.tick();
    }

    private void tick() {
        int cards = speedCardCount();
        int cycle = Math.max(2, CYCLE_TICKS - cards * 2);
        if (level == null || level.isClientSide || ++tickCounter % cycle != 0) {
            return;
        }
        if (!isRedstoneAllowed()) {
            return;
        }
        List<IItemHandler> handlers = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(direction));
            if (be != null && !(be instanceof IGridNode)) {
                be.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).ifPresent(handlers::add);
            }
        }
        machineCount = handlers.size();
        if (handlers.isEmpty() || grid == null || !grid.getState().isActive() || grid.isEcoMode() || grid.getStorage() == null) {
            return;
        }
        int budget = ITEMS_PER_CYCLE * (1 + cards);
        INetworkStorage storage = grid.getStorage();
        for (IItemHandler handler : handlers) {
            if (exportMode) {
                export(storage, handler, budget);
            } else {
                importItems(storage, handler, budget);
            }
        }
    }

    private boolean matchesFilter(ItemStack stack) {
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

    private void importItems(INetworkStorage storage, IItemHandler handler, int budget) {
        for (int slot = 0; slot < handler.getSlots() && budget > 0; slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty() || !matchesFilter(inSlot)) {
                continue;
            }
            ItemStack simulated = handler.extractItem(slot, Math.min(budget, inSlot.getCount()), true);
            if (simulated.isEmpty()) {
                continue;
            }
            ItemStack leftover = storage.insertItem(simulated.copy(), true);
            int fits = simulated.getCount() - leftover.getCount();
            if (fits <= 0) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, fits, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack rest = storage.insertItem(taken.copy(), false);
            if (!rest.isEmpty()) {
                // Мережа не прийняла (змінився стан) — повертаємо назад у машину
                ItemHandlerHelper.insertItem(handler, rest, false);
            }
            budget -= taken.getCount();
        }
    }

    private void export(INetworkStorage storage, IItemHandler handler, int budget) {
        for (int i = 0; i < FILTER_SLOTS && budget > 0; i++) {
            ItemStack wanted = filter.getStackInSlot(i);
            if (wanted.isEmpty()) {
                continue;
            }
            int have = 0;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (ItemStack.isSameItemSameTags(inSlot, wanted)) {
                    have += inSlot.getCount();
                }
            }
            int need = Math.min(budget, wanted.getCount() - have);
            if (need <= 0) {
                continue;
            }
            ItemStack extracted = storage.extractItem(wanted, need, false);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack rest = ItemHandlerHelper.insertItem(handler, extracted, false);
            budget -= extracted.getCount() - rest.getCount();
            if (!rest.isEmpty()) {
                storage.insertItem(rest, false);
            }
        }
    }

    // --- Збереження ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Filter", filter.serializeNBT());
        tag.put("Upgrades", upgrades.serializeNBT());
        tag.putBoolean("Export", exportMode);
        tag.putBoolean("Blacklist", blacklistMode);
        tag.putInt("Redstone", redstoneMode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Filter")) {
            filter.deserializeNBT(tag.getCompound("Filter"));
        }
        if (tag.contains("Upgrades")) {
            upgrades.deserializeNBT(tag.getCompound("Upgrades"));
        }
        exportMode = tag.getBoolean("Export");
        blacklistMode = tag.getBoolean("Blacklist");
        redstoneMode = tag.getInt("Redstone");
    }
}
