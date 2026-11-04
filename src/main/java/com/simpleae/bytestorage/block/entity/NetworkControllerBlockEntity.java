package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.block.NetworkControllerBlock;
import com.simpleae.bytestorage.network.GridNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import com.simpleae.bytestorage.item.WirelessTerminalItem;
import com.simpleae.bytestorage.menu.ControllerMenu;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * BlockEntity Контролера мережі (Network Controller).
 * 
 * Серце мережі Simple Byte Storage:
 * - Приймає та накопичує Forge Energy (FE).
 * - Живить підключені компоненти (диски, кабелі, термінали).
 * - Сканує мережу, координує зв'язки та виявляє конфлікти.
 */
public class NetworkControllerBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    public static final int MAX_ENERGY_CAPACITY = 100_000; // 100k FE
    public static final int MAX_ENERGY_RECEIVE = 2_000;    // 2k FE/t

    private final CustomEnergyStorage energyStorage;
    private final LazyOptional<IEnergyStorage> energyHandler;

    /** Слот привязки беспроводного терминала. */
    private final ItemStackHandler wirelessSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof WirelessTerminalItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            bindWireless();
            setChanged();
        }
    };

    /** Слот улучшения «Библиотека рецептов». */
    private final ItemStackHandler librarySlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof com.simpleae.bytestorage.item.RecipeLibraryItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private IGrid grid;
    private int tickCounter = 0;

    public NetworkControllerBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.CONTROLLER_BLOCK_ENTITY.get(), pos, state);
        this.energyStorage = new CustomEnergyStorage(MAX_ENERGY_CAPACITY, MAX_ENERGY_RECEIVE);
        this.energyHandler = LazyOptional.of(() -> this.energyStorage);
        this.grid = new GridNetwork(this);
    }

    // --- Логіка GridNode ---

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
        return 10; // Власне споживання контролера
    }

    @Override
    public boolean isController() {
        return true;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        if (level != null && !level.isClientSide) {
            BlockState currentState = getBlockState();
            if (currentState.hasProperty(NetworkControllerBlock.STATE) &&
                currentState.getValue(NetworkControllerBlock.STATE) != newState) {
                level.setBlock(worldPosition, currentState.setValue(NetworkControllerBlock.STATE, newState), 3);
            }
        }
    }

    // --- Беспроводной терминал ---

    public ItemStackHandler getLibrarySlot() {
        return librarySlot;
    }

    /** Установлена ли Библиотека рецептов (сеть знает все рецепты верстака). */
    public boolean hasRecipeLibrary() {
        return librarySlot.getStackInSlot(0).getItem() instanceof com.simpleae.bytestorage.item.RecipeLibraryItem;
    }

    public ItemStackHandler getWirelessSlot() {
        return wirelessSlot;
    }

    /** Привязать терминал, лежащий в слоте, к этому контроллеру. */
    private void bindWireless() {
        if (level == null || level.isClientSide) return;
        ItemStack stack = wirelessSlot.getStackInSlot(0);
        if (stack.getItem() instanceof WirelessTerminalItem) {
            WirelessTerminalItem.bind(stack, level, worldPosition);
        }
    }

    /** Подзарядка терминала в слоте из буфера контроллера. */
    private void chargeWireless() {
        ItemStack stack = wirelessSlot.getStackInSlot(0);
        if (!(stack.getItem() instanceof WirelessTerminalItem)) return;
        if (energyStorage.getEnergyStored() <= 0) return;
        int accepted = WirelessTerminalItem.charge(stack, Math.min(WirelessTerminalItem.CHARGE_PER_TICK, energyStorage.getEnergyStored()));
        if (accepted > 0) {
            energyStorage.consumeInternalEnergy(accepted);
            setChanged();
        }
    }

    public List<ItemStack> getDroppedItems() {
        List<ItemStack> drops = new java.util.ArrayList<>();
        ItemStack stack = wirelessSlot.getStackInSlot(0);
        if (!stack.isEmpty()) drops.add(stack.copy());
        ItemStack library = librarySlot.getStackInSlot(0);
        if (!library.isEmpty()) drops.add(library.copy());
        return drops;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ControllerMenu(containerId, playerInventory, this);
    }

    // --- Енергетичний стан ---

    public boolean hasEnoughEnergy() {
        if (grid == null) return false;
        return energyStorage.getEnergyStored() >= grid.getTotalEnergyDrain();
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    /** Забрати енергію з внутрішнього буфера контролера (для зарядки бездротового термінала). */
    public int drawEnergy(int amount) {
        return energyStorage.consumeInternalEnergy(Math.max(0, amount));
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    // --- Серверний Тікер (Server Tick) ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, NetworkControllerBlockEntity controller) {
        controller.tickCounter++;

        // Кожні 20 тіків (1 секунда) оновлюємо структуру підключених нод мережі
        if (controller.tickCounter % 20 == 0) {
            controller.scanNetworkTopology();
        }

        IGrid grid = controller.getGrid();
        if (grid == null) return;

        controller.chargeWireless();

        // Якщо стан конфлікту (більше одного контролера) — мережа зупиняється
        if (grid.getState() == GridState.CONFLICT) {
            return;
        }

        int drain = grid.getTotalEnergyDrain();
        if (controller.energyStorage.getEnergyStored() >= drain) {
            controller.energyStorage.consumeInternalEnergy(drain);
            grid.setState(GridState.ONLINE);
        } else {
            grid.setState(GridState.OFFLINE);
        }
    }

    /**
     * Пошук у ширину (BFS) для виявлення всіх з'єднаних компонентів мережі Simple Byte Storage.
     */
    public void scanNetworkTopology() {
        if (level == null || level.isClientSide) return;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        visited.add(worldPosition);
        queue.add(worldPosition);

        Set<IGridNode> foundNodes = new HashSet<>();
        foundNodes.add(this);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(direction);
                if (visited.contains(neighborPos)) continue;

                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe instanceof IGridNode node) {
                    visited.add(neighborPos);
                    foundNodes.add(node);
                    queue.add(neighborPos);
                }
            }
        }

        // Оновлюємо склад мережі
        if (grid != null) {
            // Видаляємо від'єднані ноди
            for (IGridNode oldNode : new HashSet<>(grid.getNodes())) {
                if (!foundNodes.contains(oldNode)) {
                    grid.removeNode(oldNode);
                }
            }
            // Додаємо нові ноди
            for (IGridNode newNode : foundNodes) {
                if (!grid.getNodes().contains(newNode)) {
                    grid.addNode(newNode);
                }
            }
            grid.validateNetwork();
        }
    }

    // --- Capabilities & NBT ---

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.put("Wireless", wirelessSlot.serializeNBT());
        tag.put("Library", librarySlot.serializeNBT());
        // Глобальний список шаблонів крафту живе в мережі — зберігаємо його разом із контролером.
        if (grid != null && grid.getPatternStorage() != null) {
            tag.put("Patterns", grid.getPatternStorage().save());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Library")) {
            librarySlot.deserializeNBT(tag.getCompound("Library"));
        }
        if (tag.contains("Wireless")) {
            wirelessSlot.deserializeNBT(tag.getCompound("Wireless"));
        }
        if (tag.contains("Energy")) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
        if (grid != null && grid.getPatternStorage() != null && tag.contains("Patterns", Tag.TAG_LIST)) {
            grid.getPatternStorage().load(tag.getList("Patterns", Tag.TAG_COMPOUND));
        }
    }

    // --- Кастомний клас EnergyStorage ---

    public static class CustomEnergyStorage extends EnergyStorage {
        public CustomEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0); // Зовні не можна викачувати енергію (maxExtract = 0)
        }

        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(capacity, energy));
        }

        public int consumeInternalEnergy(int amount) {
            int extracted = Math.min(this.energy, amount);
            this.energy -= extracted;
            return extracted;
        }
    }
}
