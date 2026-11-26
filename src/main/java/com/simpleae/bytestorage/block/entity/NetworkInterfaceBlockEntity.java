package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.menu.NetworkInterfaceMenu;
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

/**
 * BlockEntity Сетевого интерфейса:
 * - Любые вставляемые извне предметы сразу отправляются в хранилище сети.
 * - 9 слотов буфера автоматически пополняются из сети по 9 слотам конфигурации (Keep-in-stock).
 */
public class NetworkInterfaceBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    public static final int SLOTS = 9;
    private static final int TICK_INTERVAL = 10;

    private IGrid grid;
    private int tickCounter = 0;

    private final ItemStackHandler config = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler buffer = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final IItemHandler externalHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return buffer.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || grid == null || !grid.getState().isActive() || grid.isEcoMode()) {
                return stack;
            }
            INetworkStorage storage = grid.getStorage();
            if (storage == null) {
                return stack;
            }
            return storage.insertItem(stack.copy(), simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return buffer.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return buffer.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    };

    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> externalHandler);

    public NetworkInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.NETWORK_INTERFACE_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getConfigInventory() {
        return config;
    }

    public ItemStackHandler getBufferInventory() {
        return buffer;
    }

    public int getStatusCode() {
        if (grid == null || !grid.getState().isActive()) {
            return 0;
        }
        return grid.isEcoMode() ? 2 : 1;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NetworkInterfaceBlockEntity iface) {
        if (++iface.tickCounter < TICK_INTERVAL) {
            return;
        }
        iface.tickCounter = 0;
        iface.syncBufferWithNetwork();
    }

    private void syncBufferWithNetwork() {
        if (grid == null || !grid.getState().isActive() || grid.isEcoMode()) {
            return;
        }
        INetworkStorage storage = grid.getStorage();
        if (storage == null) {
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            ItemStack want = config.getStackInSlot(i);
            ItemStack have = buffer.getStackInSlot(i);

            if (want.isEmpty()) {
                if (!have.isEmpty()) {
                    ItemStack rem = storage.insertItem(have.copy(), false);
                    buffer.setStackInSlot(i, rem);
                }
                continue;
            }

            int target = Math.min(want.getCount(), want.getMaxStackSize());
            if (!have.isEmpty() && !ItemStack.isSameItemSameTags(have, want)) {
                ItemStack rem = storage.insertItem(have.copy(), false);
                buffer.setStackInSlot(i, rem);
                have = rem;
                if (!have.isEmpty()) {
                    continue;
                }
            }

            int current = have.isEmpty() ? 0 : have.getCount();
            if (current > target) {
                ItemStack excess = have.copy();
                excess.setCount(current - target);
                ItemStack rem = storage.insertItem(excess, false);
                int returned = excess.getCount() - rem.getCount();
                if (returned > 0) {
                    ItemStack updated = have.copy();
                    updated.shrink(returned);
                    buffer.setStackInSlot(i, updated);
                }
            } else if (current < target) {
                int needed = target - current;
                ItemStack pulled = storage.extractItem(want, needed, false);
                if (!pulled.isEmpty()) {
                    if (have.isEmpty()) {
                        buffer.setStackInSlot(i, pulled);
                    } else {
                        ItemStack merged = have.copy();
                        merged.grow(pulled.getCount());
                        buffer.setStackInSlot(i, merged);
                    }
                }
            }
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
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
        return Component.translatable("block.simple_bytestorage.network_interface");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NetworkInterfaceMenu(containerId, playerInventory, this);
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Config", config.serializeNBT());
        tag.put("Buffer", buffer.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Config")) {
            config.deserializeNBT(tag.getCompound("Config"));
        }
        if (tag.contains("Buffer")) {
            buffer.deserializeNBT(tag.getCompound("Buffer"));
        }
    }
}
