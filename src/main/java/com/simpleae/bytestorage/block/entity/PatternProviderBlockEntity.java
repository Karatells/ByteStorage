package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.item.PatternItem;
import com.simpleae.bytestorage.menu.PatternProviderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
 * Шина Шаблонів.
 *
 * Чергу замовлень наповнює Термінал Шаблонів. Кожні 10 тіків шина: 1) повертає в мережу залишки,
 * 2) забирає з сусідніх машин очікувані результати, 3) подає в машину інгредієнти наступного крафту,
 * якщо вони є в мережі і машина їх приймає.
 */
public class PatternProviderBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    public static final int SLOTS = 9;
    public static final int MAX_QUEUE = 1024;
    /**
     * Верхня межа крафтів «в роботі» (подано в машину, результат ще не забрано). Реально подачу обмежує
     * приймальна здатність машини (acceptsAll), тому ліміт великий: раніше 16 зупиняло замовлення на стак.
     */
    public static final int MAX_IN_FLIGHT = 256;
    private static final int CYCLE_TICKS = 10;
    private static final int PUSHES_PER_CYCLE = 16;

    private static final class Job {
        final String patternId;
        int left;

        Job(String patternId, int left) {
            this.patternId = patternId;
            this.left = left;
        }
    }

    private IGrid grid;
    private int tickCounter = 0;
    private int machineCount = 0;

    private final List<Job> jobs = new ArrayList<>();
    /** Очікувані результати (предмет × кількість), що ще не повернулися в мережу. */
    private final List<ItemStack> expected = new ArrayList<>();
    /** Предмети, які не вдалося вкласти в машину / мережу, — повертаються в мережу. */
    private final List<ItemStack> overflow = new ArrayList<>();

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!(stack.getItem() instanceof PatternItem item)) {
                return false;
            }
            PatternData pattern = item.getPattern(stack);
            return pattern != null && pattern.isProcessing();
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

    public PatternProviderBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.PATTERN_PROVIDER_BLOCK_ENTITY.get(), pos, state);
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
        return Component.translatable("block.simple_bytestorage.pattern_provider");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PatternProviderMenu(containerId, playerInventory, this);
    }

    // --- Шаблони та черга ---

    public IItemHandler getInventoryHandler() {
        return inventory;
    }

    /** Усі процесні шаблони, що лежать у цій шині. */
    public List<PatternData> getPatterns() {
        List<PatternData> result = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof PatternItem item) {
                PatternData pattern = item.getPattern(stack);
                if (pattern != null && pattern.isProcessing()) {
                    result.add(pattern);
                }
            }
        }
        return result;
    }

    @Nullable
    public PatternData findPattern(String id) {
        for (PatternData pattern : getPatterns()) {
            if (pattern.getId().equals(id)) {
                return pattern;
            }
        }
        return null;
    }

    public int getPendingCrafts() {
        int total = 0;
        for (Job job : jobs) {
            total += job.left;
        }
        return total;
    }

    /** Додати у чергу {@code crafts} виконань шаблону; повертає, скільки прийнято. */
    public int enqueue(String patternId, int crafts) {
        if (crafts <= 0 || findPattern(patternId) == null) {
            return 0;
        }
        int accepted = Math.min(crafts, MAX_QUEUE - getPendingCrafts());
        if (accepted <= 0) {
            return 0;
        }
        for (Job job : jobs) {
            if (job.patternId.equals(patternId)) {
                job.left += accepted;
                setChanged();
                return accepted;
            }
        }
        jobs.add(new Job(patternId, accepted));
        setChanged();
        return accepted;
    }

    /** Зведення черги: id шаблону → кількість крафтів. */
    public java.util.Map<String, Integer> getJobSummary() {
        java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
        for (Job job : jobs) {
            result.merge(job.patternId, job.left, Integer::sum);
        }
        return result;
    }

    public void cancelPattern(String patternId) {
        if (jobs.removeIf(job -> job.patternId.equals(patternId))) {
            setChanged();
        }
    }

    public int getMachineCount() {
        return machineCount;
    }

    public boolean isOnline() {
        return grid != null && grid.getState().isActive();
    }

    public void clearQueue() {
        jobs.clear();
        setChanged();
    }

    // --- Тік ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, PatternProviderBlockEntity provider) {
        provider.tick();
    }

    // --- Режим записи («обучение») ---

    public static final int REC_OFF = 0;
    public static final int REC_WAITING = 1;   // ждём, пока игрок положит ингредиенты в машину
    public static final int REC_LOADED = 2;    // ингредиенты положены, ждём начала работы
    public static final int REC_PROCESSING = 3; // машина работает, ждём результат
    public static final int REC_SAVED = 4;     // шаблон записан
    public static final int REC_NO_SLOT = 5;   // нет свободного слота для шаблона
    public static final int REC_NO_MACHINE = 6;
    public static final int REC_FAILED = 7;    // не удалось определить рецепт

    public static final int WAIT_NONE = 0;
    public static final int WAIT_RESOURCES = 1;   // в сети не хватает ингредиентов
    public static final int WAIT_NO_MACHINE = 2;  // нет машины, которая принимает ингредиенты

    private int waitReason = WAIT_NONE;

    public int getWaitReason() {
        return waitReason;
    }

    /** Результат первого задания шины (для предупреждений); пустой стак, если заданий нет. */
    public ItemStack getCurrentOutput() {
        if (jobs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        PatternData pattern = findPattern(jobs.get(0).patternId);
        return pattern == null ? ItemStack.EMPTY : pattern.getOutput();
    }

    /** Чего не хватает первому заданию; null, если всё есть. */
    @Nullable
    public Component describeMissing() {
        if (jobs.isEmpty() || grid == null || grid.getStorage() == null) {
            return null;
        }
        PatternData pattern = findPattern(jobs.get(0).patternId);
        if (pattern == null) {
            return Component.translatable("msg.simple_bytestorage.order.no_pattern");
        }
        for (ItemStack input : pattern.getInputs()) {
            ItemStack have = grid.getStorage().extractForCrafting(input, input.getCount(), true);
            int got = have.isEmpty() ? 0 : have.getCount();
            if (got < input.getCount()) {
                return Component.translatable("msg.simple_bytestorage.order.missing_short",
                        input.getCount() - got, input.getHoverName());
            }
        }
        return null;
    }

    private int recState = REC_OFF;
    private java.util.Map<String, Integer> recBase = new java.util.HashMap<>();
    private java.util.Map<String, Integer> recPrev = new java.util.HashMap<>();
    private java.util.Map<String, Integer> recStart = new java.util.HashMap<>();
    private final java.util.Map<String, ItemStack> recSample = new java.util.HashMap<>();
    private int recFinalizeIn = -1;

    public int getRecordState() {
        return recState;
    }

    public boolean isRecording() {
        return recState >= REC_WAITING && recState <= REC_PROCESSING;
    }

    /** Включить/выключить запись. Пока идёт запись, шина не подаёт и не забирает предметы сама. */
    public void toggleRecording() {
        if (isRecording()) {
            recState = REC_OFF;
            return;
        }
        List<Machine> machines = findMachines();
        if (machines.isEmpty()) {
            recState = REC_NO_MACHINE;
            return;
        }
        recSample.clear();
        recBase = snapshot(machines);
        recPrev = new java.util.HashMap<>(recBase);
        recStart = new java.util.HashMap<>();
        recFinalizeIn = -1;
        recState = REC_WAITING;
    }

    private java.util.Map<String, Integer> snapshot(List<Machine> machines) {
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        for (Machine machine : machines) {
            IItemHandler handler = machine.insertHandler();
            if (handler == null) {
                continue;
            }
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    String key = com.simpleae.bytestorage.util.CraftingService.key(stack);
                    result.merge(key, stack.getCount(), Integer::sum);
                    recSample.putIfAbsent(key, stack.copyWithCount(1));
                }
            }
        }
        return result;
    }

    /**
     * Наблюдение за машиной: после того как игрок положил ингредиенты, момент первого уменьшения запоминается;
     * когда появляется результат, расходом считается убыль предметов, а выходом — прирост.
     */
    private void recordTick() {
        List<Machine> machines = findMachines();
        machineCount = machines.size();
        if (machines.isEmpty()) {
            recState = REC_NO_MACHINE;
            return;
        }
        java.util.Map<String, Integer> cur = snapshot(machines);
        boolean decreased = false;
        boolean increasedVsBase = false;
        for (java.util.Map.Entry<String, Integer> e : recPrev.entrySet()) {
            if (cur.getOrDefault(e.getKey(), 0) < e.getValue()) {
                decreased = true;
            }
        }
        for (java.util.Map.Entry<String, Integer> e : cur.entrySet()) {
            if (e.getValue() > recBase.getOrDefault(e.getKey(), 0)) {
                increasedVsBase = true;
            }
        }
        switch (recState) {
            case REC_WAITING -> {
                if (decreased) {
                    // игрок забрал предметы — начинаем с нового исходного состояния
                    recBase = cur;
                } else if (increasedVsBase) {
                    recState = REC_LOADED;
                }
            }
            case REC_LOADED -> {
                if (decreased) {
                    recStart = new java.util.HashMap<>(recPrev);
                    recState = REC_PROCESSING;
                }
            }
            case REC_PROCESSING -> {
                if (recFinalizeIn < 0) {
                    for (java.util.Map.Entry<String, Integer> e : cur.entrySet()) {
                        if (e.getValue() > recStart.getOrDefault(e.getKey(), 0)) {
                            recFinalizeIn = 4; // небольшая задержка: дождаться остальных результатов
                            break;
                        }
                    }
                } else if (--recFinalizeIn <= 0) {
                    finishRecording(cur);
                }
            }
            default -> {
            }
        }
        recPrev = cur;
    }

    private void finishRecording(java.util.Map<String, Integer> cur) {
        java.util.Map<String, Integer> consumed = new java.util.LinkedHashMap<>();
        String outKey = null;
        int outCount = 0;
        for (java.util.Map.Entry<String, Integer> e : recStart.entrySet()) {
            int now = cur.getOrDefault(e.getKey(), 0);
            if (now < e.getValue()) {
                consumed.put(e.getKey(), e.getValue() - now);
            }
        }
        for (java.util.Map.Entry<String, Integer> e : cur.entrySet()) {
            int gain = e.getValue() - recStart.getOrDefault(e.getKey(), 0);
            if (gain > outCount && !consumed.containsKey(e.getKey())) {
                outCount = gain;
                outKey = e.getKey();
            }
        }
        if (outKey == null || consumed.isEmpty() || consumed.size() > PatternData.GRID_SIZE) {
            recState = REC_FAILED;
            return;
        }
        ItemStack output = recSample.get(outKey).copyWithCount(outCount);
        List<ItemStack> inputs = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : consumed.entrySet()) {
            ItemStack sample = recSample.get(e.getKey());
            if (sample == null) {
                recState = REC_FAILED;
                return;
            }
            inputs.add(sample.copyWithCount(e.getValue()));
        }
        int free = -1;
        for (int i = 0; i < SLOTS; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                free = i;
                break;
            }
        }
        if (free < 0) {
            recState = REC_NO_SLOT;
            return;
        }
        ItemStack pattern = new ItemStack(SimpleByteStorage.PATTERN_ITEM.get());
        ((PatternItem) pattern.getItem()).program(pattern, PatternData.processing(output, inputs));
        inventory.setStackInSlot(free, pattern);
        setChanged();
        recState = REC_SAVED;
    }

    private void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (isRecording()) {
            recordTick();
            return;
        }
        if (++tickCounter % CYCLE_TICKS != 0) {
            return;
        }
        List<Machine> machines = findMachines();
        machineCount = machines.size();

        if (grid == null || !grid.getState().isActive() || grid.getStorage() == null) {
            return;
        }
        // Шина заряжает соседние машины энергией сети (электропечь и т.п.)
        com.simpleae.bytestorage.util.NetworkPower.feed(grid, level, worldPosition, CYCLE_TICKS);
        INetworkStorage storage = grid.getStorage();

        flushOverflow(storage);
        collectOutputs(storage, machines);

        waitReason = WAIT_NONE;
        if (jobs.isEmpty()) {
            return;
        }
        if (machines.isEmpty()) {
            waitReason = WAIT_NO_MACHINE;
            return;
        }
        for (int i = 0; i < PUSHES_PER_CYCLE && !jobs.isEmpty(); i++) {
            if (!pushOne(storage, machines)) {
                break;
            }
        }
    }

    /** Сусідні блоки з інвентарем, які не є нодами мережі. */
    private List<Machine> findMachines() {
        List<Machine> result = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(direction));
            if (be != null && !(be instanceof IGridNode)
                    && be.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).isPresent()) {
                result.add(new Machine(be, direction.getOpposite()));
            }
        }
        return result;
    }

    /** Сусідня машина і її грань, що дивиться на шину. */
    private record Machine(BlockEntity be, Direction side) {
        IItemHandler insertHandler() {
            return be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
        }
    }

    private void flushOverflow(INetworkStorage storage) {
        for (int i = overflow.size() - 1; i >= 0; i--) {
            ItemStack rest = storage.insertItem(overflow.get(i).copy(), false);
            if (rest.isEmpty()) {
                overflow.remove(i);
                setChanged();
            } else if (rest.getCount() != overflow.get(i).getCount()) {
                overflow.set(i, rest);
                setChanged();
            }
        }
    }

    /** Забирає з машини (з усіх її сторін) готові предмети, яких ми очікуємо, і кладе в мережу. */
    private void collectOutputs(INetworkStorage storage, List<Machine> machines) {
        if (expected.isEmpty()) {
            return;
        }
        for (Machine machine : machines) {
            List<IItemHandler> handlers = new ArrayList<>();
            for (Direction side : Direction.values()) {
                machine.be().getCapability(ForgeCapabilities.ITEM_HANDLER, side).ifPresent(handlers::add);
            }
            for (IItemHandler handler : handlers) {
                for (int slot = 0; slot < handler.getSlots() && !expected.isEmpty(); slot++) {
                    ItemStack inSlot = handler.getStackInSlot(slot);
                    if (inSlot.isEmpty()) {
                        continue;
                    }
                    ItemStack want = findExpected(inSlot);
                    if (want == null) {
                        continue;
                    }
                    ItemStack simulated = handler.extractItem(slot, Math.min(want.getCount(), inSlot.getCount()), true);
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
                        addTo(overflow, rest);
                    }
                    want.shrink(taken.getCount());
                    if (want.getCount() <= 0) {
                        expected.remove(want);
                    }
                    setChanged();
                }
            }
        }
    }

    @Nullable
    private ItemStack findExpected(ItemStack stack) {
        for (ItemStack entry : expected) {
            if (ItemStack.isSameItemSameTags(entry, stack)) {
                return entry;
            }
        }
        return null;
    }

    private int expectedTotal() {
        int total = 0;
        for (ItemStack entry : expected) {
            total += entry.getCount();
        }
        return total;
    }

    /** Подає в машину інгредієнти одного крафту. false — зараз неможливо (чекаємо). */
    private boolean pushOne(INetworkStorage storage, List<Machine> machines) {
        List<String> ids = new ArrayList<>(jobs.size());
        for (Job j : jobs) {
            ids.add(j.patternId);
        }
        int pick = com.simpleae.bytestorage.util.CraftingService.pickJob(grid, ids);
        if (pick < 0) {
            return false; // усі призупинені
        }
        if (pick > 0) {
            jobs.add(0, jobs.remove(pick));
        }
        Job job = jobs.get(0);
        PatternData pattern = findPattern(job.patternId);
        if (pattern == null) {
            jobs.remove(0);
            setChanged();
            return true;
        }
        if (expectedTotal() >= Math.max(1, pattern.getOutput().getCount()) * MAX_IN_FLIGHT) {
            return false;
        }
        for (ItemStack input : pattern.getInputs()) {
            ItemStack available = storage.extractForCrafting(input, input.getCount(), true);
            if (available.isEmpty() || available.getCount() < input.getCount()) {
                waitReason = WAIT_RESOURCES;
                return false;
            }
        }

        for (Machine machine : machines) {
            IItemHandler handler = machine.insertHandler();
            if (handler == null || !acceptsAll(handler, pattern.getInputs())) {
                continue;
            }
            for (ItemStack input : pattern.getInputs()) {
                ItemStack extracted = storage.extractForCrafting(input, input.getCount(), false);
                if (extracted.isEmpty()) {
                    continue;
                }
                ItemStack rest = ItemHandlerHelper.insertItem(handler, extracted, false);
                if (!rest.isEmpty()) {
                    addTo(overflow, rest);
                }
            }
            addTo(expected, pattern.getOutput());
            com.simpleae.bytestorage.util.NetworkJournal.log(grid, com.simpleae.bytestorage.util.JournalLog.STARTED,
                    pattern.getOutput(), pattern.getOutput().getCount());
            job.left--;
            if (job.left <= 0) {
                jobs.remove(0);
            }
            waitReason = WAIT_NONE;
            setChanged();
            return true;
        }
        waitReason = WAIT_NO_MACHINE;
        return false;
    }

    private static boolean acceptsAll(IItemHandler handler, List<ItemStack> inputs) {
        for (ItemStack input : inputs) {
            ItemStack rest = ItemHandlerHelper.insertItem(handler, input.copy(), true);
            if (!rest.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void addTo(List<ItemStack> list, ItemStack stack) {
        for (ItemStack entry : list) {
            if (ItemStack.isSameItemSameTags(entry, stack)) {
                entry.grow(stack.getCount());
                return;
            }
        }
        list.add(stack.copy());
    }

    /** Предмети, які треба повернути гравцеві при руйнуванні блоку. */
    public List<ItemStack> getDroppedItems() {
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return drops;
    }

    // --- Збереження (кількості пишемо окремим int, бо Count у ItemStack.save — байт) ---

    private static ListTag saveCounted(List<ItemStack> list) {
        ListTag tag = new ListTag();
        for (ItemStack stack : list) {
            ItemStack one = stack.copy();
            one.setCount(1);
            CompoundTag entry = one.save(new CompoundTag());
            entry.putInt("N", stack.getCount());
            tag.add(entry);
        }
        return tag;
    }

    private static void loadCounted(ListTag tag, List<ItemStack> into) {
        into.clear();
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entry = tag.getCompound(i);
            ItemStack stack = ItemStack.of(entry);
            int count = entry.getInt("N");
            if (!stack.isEmpty() && count > 0) {
                stack.setCount(count);
                into.add(stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Patterns", inventory.serializeNBT());
        ListTag jobList = new ListTag();
        for (Job job : jobs) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", job.patternId);
            entry.putInt("Left", job.left);
            jobList.add(entry);
        }
        tag.put("Jobs", jobList);
        tag.put("Expected", saveCounted(expected));
        tag.put("Overflow", saveCounted(overflow));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Patterns")) {
            inventory.deserializeNBT(tag.getCompound("Patterns"));
        }
        jobs.clear();
        ListTag jobList = tag.getList("Jobs", Tag.TAG_COMPOUND);
        for (int i = 0; i < jobList.size(); i++) {
            CompoundTag entry = jobList.getCompound(i);
            if (entry.getInt("Left") > 0) {
                jobs.add(new Job(entry.getString("Id"), entry.getInt("Left")));
            }
        }
        loadCounted(tag.getList("Expected", Tag.TAG_COMPOUND), expected);
        loadCounted(tag.getList("Overflow", Tag.TAG_COMPOUND), overflow);
    }
}
