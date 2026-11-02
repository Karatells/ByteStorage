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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * BlockEntity Контролера мережі (Network Controller).
 * 
 * Серце мережі SimpleAE:
 * - Приймає та накопичує Forge Energy (FE).
 * - Живить підключені компоненти (диски, кабелі, термінали).
 * - Сканує мережу, координує зв'язки та виявляє конфлікти.
 */
public class NetworkControllerBlockEntity extends BlockEntity implements IGridNode {

    public static final int MAX_ENERGY_CAPACITY = 100_000; // 100k FE
    public static final int MAX_ENERGY_RECEIVE = 2_000;    // 2k FE/t

    private final CustomEnergyStorage energyStorage;
    private final LazyOptional<IEnergyStorage> energyHandler;

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

    // --- Енергетичний стан ---

    public boolean hasEnoughEnergy() {
        if (grid == null) return false;
        return energyStorage.getEnergyStored() >= grid.getTotalEnergyDrain();
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
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
     * Пошук у ширину (BFS) для виявлення всіх з'єднаних компонентів мережі SimpleAE.
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
        // Глобальний список шаблонів крафту живе в мережі — зберігаємо його разом із контролером.
        if (grid != null && grid.getPatternStorage() != null) {
            tag.put("Patterns", grid.getPatternStorage().save());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
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
