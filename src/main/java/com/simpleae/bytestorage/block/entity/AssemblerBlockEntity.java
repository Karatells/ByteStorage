package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.IByteStorageCell;
import com.simpleae.bytestorage.api.storage.ICellInventory;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.menu.AssemblerMenu;
import com.simpleae.bytestorage.util.PatternCrafting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Асемблер — автоматичний верстак МЕ-мережі.
 *
 * Принцип роботи:
 *  1. У слот вставляється карточка пам'яті — це робочий буфер Асемблера.
 *  2. Замовлення (з Термінала Шаблонів) стають у чергу.
 *  3. Для першого замовлення Асемблер копіює з мережі у карточку всі потрібні інгредієнти.
 *  4. Після затримки крафту інгредієнти за розкладкою шаблону вилучаються з карточки,
 *     виконується ванільний рецепт, а результат (і залишки типу відер) потрапляє в ту саму карточку.
 *  5. Вміст карточки повертається в основну мережу; далі наступний крафт з черги.
 */
public class AssemblerBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    public static final int BUFFER_SLOT = 0;
    public static final int CRAFT_TICKS_PER_RECIPE = 40;
    public static final int MAX_QUEUE = 9999;

    // Фази
    public static final int PHASE_IDLE = 0;
    public static final int PHASE_CRAFTING = 1;
    public static final int PHASE_DELIVERING = 2;

    // Причини очікування (для статусу в GUI)
    public static final int WAIT_NONE = 0;
    public static final int WAIT_NO_CARD = 1;
    public static final int WAIT_RESOURCES = 2;
    public static final int WAIT_CARD_BUSY = 3;
    public static final int WAIT_CARD_SMALL = 4;
    public static final int WAIT_NETWORK_OFF = 5;
    public static final int WAIT_NETWORK_FULL = 6;

    // Індекси ContainerData
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_PROGRESS_MAX = 1;
    public static final int DATA_PHASE = 2;
    public static final int DATA_QUEUE = 3;
    public static final int DATA_WAIT = 4;
    public static final int DATA_COUNT = 5;

    private static final class Job {
        final String patternId;
        int count;

        Job(String patternId, int count) {
            this.patternId = patternId;
            this.count = count;
        }
    }

    private IGrid grid;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof IByteStorageCell;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final List<Job> jobs = new ArrayList<>();
    private int phase = PHASE_IDLE;
    private int progress = 0;
    private int waitReason = WAIT_NONE;
    private ItemStack currentOutput = ItemStack.EMPTY;
    private int tickCounter = 0;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROGRESS_MAX -> CRAFT_TICKS_PER_RECIPE;
                case DATA_PHASE -> phase;
                case DATA_QUEUE -> getPendingCrafts();
                case DATA_WAIT -> waitReason;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Значення керуються лише сервером.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.ASSEMBLER_BLOCK_ENTITY.get(), pos, state);
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
        return 3;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.assembler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AssemblerMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- Доступ для меню ---

    public IItemHandler getInventoryHandler() {
        return inventory;
    }

    public ItemStack getBufferCell() {
        return inventory.getStackInSlot(BUFFER_SLOT);
    }

    public boolean hasBufferCell() {
        ItemStack stack = getBufferCell();
        return !stack.isEmpty() && stack.getItem() instanceof IByteStorageCell;
    }

    public int getPhase() {
        return phase;
    }

    public boolean isBusy() {
        return phase != PHASE_IDLE;
    }

    public ItemStack getCurrentOutput() {
        return currentOutput;
    }

    public int getPendingCrafts() {
        int total = 0;
        for (Job job : jobs) {
            total += job.count;
        }
        return Math.min(total, MAX_QUEUE);
    }

    // --- Замовлення ---

    /**
     * Додати у чергу {@code count} крафтів шаблону.
     *
     * @return скільки крафтів реально прийнято
     */
    public int enqueue(String patternId, int count) {
        int room = MAX_QUEUE - getPendingCrafts();
        int add = Math.min(room, count);
        if (add <= 0) {
            return 0;
        }
        if (!jobs.isEmpty() && jobs.get(jobs.size() - 1).patternId.equals(patternId)) {
            jobs.get(jobs.size() - 1).count += add;
        } else {
            jobs.add(new Job(patternId, add));
        }
        syncToClient();
        return add;
    }

    /**
     * Скасувати всі замовлення. Вміст карточки (інгредієнти/результат) повертається в мережу.
     */
    public void cancelJobs() {
        jobs.clear();
        progress = 0;
        currentOutput = ItemStack.EMPTY;
        ICellInventory card = cardInventory();
        phase = (card != null && !card.isEmpty()) ? PHASE_DELIVERING : PHASE_IDLE;
        waitReason = WAIT_NONE;
        syncToClient();
    }

    /** Зведення черги: id шаблону → кількість крафтів (порядок черги). */
    public java.util.Map<String, Integer> getJobSummary() {
        java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
        for (Job job : jobs) {
            result.merge(job.patternId, job.count, Integer::sum);
        }
        return result;
    }

    /** Чи виконується зараз крафт цього шаблону. */
    public boolean isWorkingOn(String patternId) {
        return phase != PHASE_IDLE && !jobs.isEmpty() && jobs.get(0).patternId.equals(patternId);
    }

    /** Скасувати всі замовлення одного шаблону (поточний крафт зупиняється, вміст карточки повертається в мережу). */
    public void cancelPattern(String patternId) {
        boolean head = isWorkingOn(patternId);
        jobs.removeIf(job -> job.patternId.equals(patternId));
        if (head) {
            progress = 0;
            currentOutput = ItemStack.EMPTY;
            ICellInventory card = cardInventory();
            phase = (card != null && !card.isEmpty()) ? PHASE_DELIVERING : PHASE_IDLE;
            waitReason = WAIT_NONE;
        }
        syncToClient();
    }

    // --- Серверний тік ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, AssemblerBlockEntity assembler) {
        assembler.tick();
    }

    private void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        tickCounter++;

        if (grid == null || !grid.getState().isActive()) {
            setWait(WAIT_NETWORK_OFF);
            return;
        }

        switch (phase) {
            case PHASE_CRAFTING -> {
                setWait(WAIT_NONE);
                progress++;
                if (progress >= CRAFT_TICKS_PER_RECIPE) {
                    finishCraft();
                }
            }
            case PHASE_DELIVERING -> {
                if (tickCounter % 5 == 0) {
                    deliver();
                }
            }
            default -> {
                if (jobs.isEmpty()) {
                    setWait(WAIT_NONE);
                } else if (tickCounter % 10 == 0) {
                    tryStartCraft();
                }
            }
        }
    }

    private void setWait(int reason) {
        if (this.waitReason != reason) {
            this.waitReason = reason;
        }
    }

    @Nullable
    private ICellInventory cardInventory() {
        ItemStack stack = getBufferCell();
        if (!stack.isEmpty() && stack.getItem() instanceof IByteStorageCell cell) {
            return cell.getCellInventory(stack);
        }
        return null;
    }

    private void dropFirstJob() {
        if (!jobs.isEmpty()) {
            jobs.remove(0);
        }
        currentOutput = ItemStack.EMPTY;
        syncToClient();
    }

    /** Крок 3: скопіювати з мережі в карточку всі інгредієнти для одного крафту. */
    private void tryStartCraft() {
        ICellInventory card = cardInventory();
        if (card == null) {
            setWait(WAIT_NO_CARD);
            return;
        }
        if (!card.isEmpty()) {
            setWait(WAIT_CARD_BUSY);
            return;
        }

        Job job = jobs.get(0);
        PatternData pattern = grid.getPatternStorage() == null ? null : grid.getPatternStorage().getPattern(job.patternId);
        if (pattern == null) {
            // Шаблон видалили з мережі — замовлення втратило сенс.
            dropFirstJob();
            return;
        }

        TransientCraftingContainer container = PatternCrafting.makeContainer(pattern.getGrid());
        Optional<CraftingRecipe> recipe = PatternCrafting.findRecipe(level, container);
        if (recipe.isEmpty()) {
            dropFirstJob();
            return;
        }
        ItemStack result = recipe.get().assemble(container, level.registryAccess());
        int outputBytes = result.getCount();
        for (ItemStack remaining : PatternCrafting.remainingItems(recipe.get(), container)) {
            outputBytes += remaining.getCount();
        }
        if (card.getFreeBytes() < Math.max(pattern.getTotalInputCount(), outputBytes)) {
            setWait(WAIT_CARD_SMALL);
            return;
        }

        INetworkStorage storage = grid.getStorage();
        for (ItemStack input : pattern.getInputs()) {
            ItemStack simulated = storage.extractItem(input, input.getCount(), true);
            if (simulated.isEmpty() || simulated.getCount() < input.getCount()) {
                setWait(WAIT_RESOURCES);
                return;
            }
        }

        List<ItemStack> taken = new ArrayList<>();
        boolean ok = true;
        for (ItemStack input : pattern.getInputs()) {
            ItemStack got = storage.extractItem(input, input.getCount(), false);
            if (!got.isEmpty()) {
                taken.add(got);
            }
            if (got.isEmpty() || got.getCount() < input.getCount()) {
                ok = false;
                break;
            }
        }
        if (ok) {
            for (ItemStack stack : taken) {
                if (!card.insertItem(stack.copy(), false).isEmpty()) {
                    ok = false;
                    break;
                }
            }
        }
        if (!ok) {
            // Відкат: усе назад у мережу.
            card.clear();
            for (ItemStack stack : taken) {
                storage.insertItem(stack, false);
            }
            setWait(WAIT_RESOURCES);
            setChanged();
            return;
        }

        currentOutput = result.copy();
        phase = PHASE_CRAFTING;
        progress = 0;
        setWait(WAIT_NONE);
        syncToClient();
    }

    /** Крок 4: виконати рецепт з вмісту карточки; результат кладемо назад у карточку. */
    private void finishCraft() {
        ICellInventory card = cardInventory();
        Job job = jobs.isEmpty() ? null : jobs.get(0);
        PatternData pattern = (job == null || grid.getPatternStorage() == null)
                ? null : grid.getPatternStorage().getPattern(job.patternId);

        boolean success = false;
        if (card != null && pattern != null) {
            boolean consumed = true;
            for (ItemStack cell : pattern.getGrid()) {
                if (!cell.isEmpty() && card.extractItem(cell, 1, false).isEmpty()) {
                    consumed = false;
                }
            }
            TransientCraftingContainer container = PatternCrafting.makeContainer(pattern.getGrid());
            Optional<CraftingRecipe> recipe = PatternCrafting.findRecipe(level, container);
            if (consumed && recipe.isPresent()) {
                ItemStack result = recipe.get().assemble(container, level.registryAccess());
                List<ItemStack> outputs = new ArrayList<>();
                outputs.add(result);
                outputs.addAll(PatternCrafting.remainingItems(recipe.get(), container));
                for (ItemStack output : outputs) {
                    if (output.isEmpty()) {
                        continue;
                    }
                    ItemStack rest = card.insertItem(output.copy(), false);
                    if (!rest.isEmpty()) {
                        rest = grid.getStorage().insertItem(rest, false);
                    }
                    if (!rest.isEmpty() && level != null) {
                        Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                                worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, rest);
                    }
                }
                success = true;
            }
        }

        if (success) {
            job.count--;
            if (job.count <= 0) {
                jobs.remove(0);
            }
        } else if (!jobs.isEmpty()) {
            jobs.remove(0);
        }

        progress = 0;
        phase = PHASE_DELIVERING;
        currentOutput = ItemStack.EMPTY;
        syncToClient();
    }

    /** Крок 5: повернути вміст карточки в основну мережу. */
    private void deliver() {
        ICellInventory card = cardInventory();
        if (card == null) {
            // Карточку забрали — повертати нічого.
            phase = PHASE_IDLE;
            syncToClient();
            return;
        }

        INetworkStorage storage = grid.getStorage();
        for (ItemStack stored : new ArrayList<>(card.getStoredItems())) {
            ItemStack copy = stored.copy();
            ItemStack rest = storage.insertItem(copy.copy(), false);
            int moved = copy.getCount() - rest.getCount();
            if (moved > 0) {
                card.extractItem(copy, moved, false);
            }
        }
        setChanged();

        if (card.isEmpty()) {
            phase = PHASE_IDLE;
            setWait(WAIT_NONE);
            syncToClient();
        } else {
            setWait(WAIT_NETWORK_FULL);
        }
    }

    // --- Синхронізація з клієнтом ---

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("Current", currentOutput.save(new CompoundTag()));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- Збереження ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Phase", phase);
        tag.putInt("Progress", progress);
        tag.put("Current", currentOutput.save(new CompoundTag()));
        ListTag list = new ListTag();
        for (Job job : jobs) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", job.patternId);
            entry.putInt("Count", job.count);
            list.add(entry);
        }
        tag.put("Jobs", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        if (tag.contains("Phase")) {
            phase = tag.getInt("Phase");
            progress = tag.getInt("Progress");
        }
        if (tag.contains("Current")) {
            currentOutput = ItemStack.of(tag.getCompound("Current"));
        }
        if (tag.contains("Jobs")) {
            jobs.clear();
            ListTag list = tag.getList("Jobs", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int count = entry.getInt("Count");
                if (count > 0) {
                    jobs.add(new Job(entry.getString("Id"), count));
                }
            }
        }
    }
}
