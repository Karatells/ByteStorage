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

    public static final int RECIPE_SETTING_SLOTS = 9;

    /** Чёрный список рецептов Библиотеки: рецепты, результат которых указан здесь, сети недоступны (призрачные слоты). */
    private final ItemStackHandler recipeBlacklist = new ItemStackHandler(RECIPE_SETTING_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            com.simpleae.bytestorage.util.RecipeLibrary.invalidate();
        }
    };

    /** Предпочитаемые варианты для инградиентов-тегов (призрачные слоты): выбираются раньше, чем «чего больше в сети». */
    private final ItemStackHandler recipePrefs = new ItemStackHandler(RECIPE_SETTING_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            com.simpleae.bytestorage.util.RecipeLibrary.invalidate();
        }
    };

    /** Приоритеты и пауза заказов (по id шаблона): сначала крафтится заказ с большим приоритетом, поставленные на паузу не стартуют. */
    private final java.util.Map<String, Integer> queuePriority = new java.util.HashMap<>();
    private final java.util.Set<String> queuePaused = new java.util.HashSet<>();
    public static final int MAX_PRIORITY = 9;

    public int getQueuePriority(String patternId) {
        return queuePriority.getOrDefault(patternId, 0);
    }

    public boolean isQueuePaused(String patternId) {
        return queuePaused.contains(patternId);
    }

    public void changeQueuePriority(String patternId, int delta) {
        int value = Math.max(-MAX_PRIORITY, Math.min(MAX_PRIORITY, getQueuePriority(patternId) + delta));
        if (value == 0) {
            queuePriority.remove(patternId);
        } else {
            queuePriority.put(patternId, value);
        }
        setChanged();
    }

    public void setQueuePaused(String patternId, boolean paused) {
        if (paused ? queuePaused.add(patternId) : queuePaused.remove(patternId)) {
            setChanged();
        }
    }

    public void setQueuePriority(String patternId, int value) {
        changeQueuePriority(patternId, value - getQueuePriority(patternId));
    }

    public void toggleQueuePaused(String patternId) {
        if (!queuePaused.remove(patternId)) {
            queuePaused.add(patternId);
        }
        setChanged();
    }

    private final com.simpleae.bytestorage.util.JournalLog journal = new com.simpleae.bytestorage.util.JournalLog();

    public com.simpleae.bytestorage.util.JournalLog getJournal() {
        return journal;
    }

    public static final int STOCK_RULES = 8;
    public static final int STOCK_OFF = 0;
    public static final int STOCK_OK = 1;
    public static final int STOCK_CRAFTING = 2;
    public static final int STOCK_FAILED = 3;
    public static final int STOCK_MAX = 1_000_000;

    /** Правила запаса: предмет (призрачный слот) + минимальное количество; сеть сама заказывает недостающее. */
    private final ItemStackHandler stockItems = new ItemStackHandler(STOCK_RULES) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final int[] stockMin = new int[STOCK_RULES];
    private final int[] stockHave = new int[STOCK_RULES];
    private final int[] stockState = new int[STOCK_RULES];
    private final int[] stockFails = new int[STOCK_RULES];
    private final net.minecraft.network.chat.Component[] stockReason = new net.minecraft.network.chat.Component[STOCK_RULES];

    /** Не чаще одного предупреждения на причину за 5 минут. */
    private static final long ALERT_COOLDOWN = 6000;
    private static final int ALERT_AFTER_FAILS = 3;
    private static final int STUCK_AFTER_TICKS = 600;
    private final java.util.Map<String, Long> alertTimes = new java.util.HashMap<>();
    private final java.util.Map<Long, Integer> stuckTicks = new java.util.HashMap<>();

    public net.minecraft.network.chat.Component getStockReason(int rule) {
        return stockReason[rule];
    }

    private void alert(String key, net.minecraft.network.chat.Component message) {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        Long last = alertTimes.get(key);
        if (last != null && now - last < ALERT_COOLDOWN) {
            return;
        }
        alertTimes.put(key, now);
        com.simpleae.bytestorage.util.NetworkAlerts.notifyOwners(this, message);
    }

    /** Раз в 3 секунды: машины, которые давно ждут ресурсы или место, попадают в журнал и в предупреждение. */
    private void checkStuck(IGrid grid) {
        java.util.Set<Long> waiting = new java.util.HashSet<>();
        for (com.simpleae.bytestorage.api.network.IGridNode node : grid.getNodes()) {
            if (!(node instanceof AssemblerBlockEntity assembler) || assembler.getPendingCrafts() <= 0) {
                continue;
            }
            int reason = assembler.getWaitReason();
            if (reason != AssemblerBlockEntity.WAIT_RESOURCES && reason != AssemblerBlockEntity.WAIT_NETWORK_FULL) {
                continue;
            }
            long id = assembler.getBlockPos().asLong();
            waiting.add(id);
            int ticks = stuckTicks.merge(id, 60, Integer::sum);
            if (ticks < STUCK_AFTER_TICKS) {
                continue;
            }
            net.minecraft.network.chat.Component why = reason == AssemblerBlockEntity.WAIT_NETWORK_FULL
                    ? net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.alert.storage_full")
                    : assembler.describeMissing();
            if (why == null) {
                continue;
            }
            net.minecraft.world.item.ItemStack out = assembler.getCurrentOutput();
            net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable(
                    "msg.simple_bytestorage.alert.stuck", out.isEmpty() ? "?" : out.getHoverName(), why);
            String key = "stuck:" + id;
            Long last = alertTimes.get(key);
            if (last == null || level == null || level.getGameTime() - last >= ALERT_COOLDOWN) {
                com.simpleae.bytestorage.util.NetworkJournal.log(grid, com.simpleae.bytestorage.util.JournalLog.STUCK,
                        out, Math.max(1, assembler.getPendingCrafts()));
            }
            alert(key, message);
        }
        for (com.simpleae.bytestorage.api.network.IGridNode node : grid.getNodes()) {
            if (!(node instanceof PatternProviderBlockEntity bus) || bus.getPendingCrafts() <= 0) {
                continue;
            }
            int reason = bus.getWaitReason();
            if (reason == PatternProviderBlockEntity.WAIT_NONE) {
                continue;
            }
            long id = bus.getBlockPos().asLong();
            waiting.add(id);
            int ticks = stuckTicks.merge(id, 60, Integer::sum);
            // машина может законно занимать время (долгий рецепт), поэтому для неё порог вдвое больше
            int limit = reason == PatternProviderBlockEntity.WAIT_NO_MACHINE ? STUCK_AFTER_TICKS * 2 : STUCK_AFTER_TICKS;
            if (ticks < limit) {
                continue;
            }
            net.minecraft.network.chat.Component why = reason == PatternProviderBlockEntity.WAIT_NO_MACHINE
                    ? net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.alert.no_machine")
                    : bus.describeMissing();
            if (why == null) {
                continue;
            }
            net.minecraft.world.item.ItemStack out = bus.getCurrentOutput();
            net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable(
                    "msg.simple_bytestorage.alert.stuck", out.isEmpty() ? "?" : out.getHoverName(), why);
            String key = "stuck:" + id;
            Long last = alertTimes.get(key);
            if (last == null || level == null || level.getGameTime() - last >= ALERT_COOLDOWN) {
                com.simpleae.bytestorage.util.NetworkJournal.log(grid, com.simpleae.bytestorage.util.JournalLog.STUCK,
                        out, Math.max(1, bus.getPendingCrafts()));
            }
            alert(key, message);
        }
        stuckTicks.keySet().retainAll(waiting);
    }


    public ItemStackHandler getStockItems() {
        return stockItems;
    }

    public int getStockMin(int rule) {
        return stockMin[rule];
    }

    public int getStockHave(int rule) {
        return stockHave[rule];
    }

    public int getStockState(int rule) {
        return stockState[rule];
    }

    /** Задать правило запаса программно (скрипты): пустой предмет или minimum <= 0 очищает правило. */
    public void setStockRule(int rule, ItemStack item, int minimum) {
        if (rule < 0 || rule >= STOCK_RULES) {
            return;
        }
        if (item.isEmpty() || minimum <= 0) {
            stockItems.setStackInSlot(rule, ItemStack.EMPTY);
            stockMin[rule] = 0;
        } else {
            stockItems.setStackInSlot(rule, item.copyWithCount(1));
            stockMin[rule] = Math.min(STOCK_MAX, minimum);
        }
        stockState[rule] = STOCK_OFF;
        stockFails[rule] = 0;
        stockReason[rule] = null;
        setChanged();
    }

    /** Предупреждение от скрипта: не чаще раза в 30 секунд на источник. */
    public boolean scriptAlert(String sourceKey, net.minecraft.network.chat.Component message) {
        if (level == null) {
            return false;
        }
        long now = level.getGameTime();
        Long last = alertTimes.get(sourceKey);
        if (last != null && now - last < 600) {
            return false;
        }
        alertTimes.put(sourceKey, now);
        return com.simpleae.bytestorage.util.NetworkAlerts.notifyOwners(this, message) >= 0;
    }

    public void changeStockMin(int rule, int delta) {
        if (rule < 0 || rule >= STOCK_RULES) {
            return;
        }
        stockMin[rule] = Math.max(0, Math.min(STOCK_MAX, stockMin[rule] + delta));
        setChanged();
    }

    /** Проверка правил запаса: недостающее заказывается как автокрафт (если такой заказ уже в очереди — ждём). */
    private void runStockRules(IGrid grid) {
        com.simpleae.bytestorage.api.storage.INetworkStorage storage = grid.getStorage();
        if (storage == null) {
            return;
        }
        int energyPercent = energyStorage.getMaxEnergyStored() <= 0 ? 0
                : (int) (energyStorage.getEnergyStored() * 100L / energyStorage.getMaxEnergyStored());
        stockActive = com.simpleae.bytestorage.util.StockSchedule.isActive(stockMode,
                level != null ? level.getDayTime() : 0, energyPercent, stockActive);
        if (!stockActive) {
            return; // правила приостановлены расписанием: новых заказов нет
        }
        java.util.Map<String, Integer> have = new java.util.HashMap<>();
        for (ItemStack stack : storage.getAllItems()) {
            have.merge(com.simpleae.bytestorage.util.CraftingService.key(stack), stack.getCount(), Integer::sum);
        }
        java.util.Set<String> queued = null;
        for (int i = 0; i < STOCK_RULES; i++) {
            ItemStack wanted = stockItems.getStackInSlot(i);
            if (wanted.isEmpty() || stockMin[i] <= 0) {
                stockState[i] = STOCK_OFF;
                stockHave[i] = 0;
                stockFails[i] = 0;
                stockReason[i] = null;
                continue;
            }
            String key = com.simpleae.bytestorage.util.CraftingService.key(wanted);
            int current = have.getOrDefault(key, 0);
            stockHave[i] = current;
            if (current >= stockMin[i]) {
                stockState[i] = STOCK_OK;
                stockFails[i] = 0;
                stockReason[i] = null;
                continue;
            }
            if (queued == null) {
                queued = new java.util.HashSet<>();
                for (com.simpleae.bytestorage.util.CraftingService.QueueEntry e : com.simpleae.bytestorage.util.CraftingService.queue(grid)) {
                    queued.add(com.simpleae.bytestorage.util.CraftingService.key(e.pattern.getOutput()));
                }
            }
            if (queued.contains(key)) {
                stockState[i] = STOCK_CRAFTING;
                stockFails[i] = 0;
                stockReason[i] = null;
                continue;
            }
            int got = com.simpleae.bytestorage.util.CraftingService.requestCraft(grid, wanted, stockMin[i] - current);
            int previous = stockState[i];
            stockState[i] = got > 0 ? STOCK_CRAFTING : STOCK_FAILED;
            if (got > 0) {
                stockFails[i] = 0;
                stockReason[i] = null;
                com.simpleae.bytestorage.util.NetworkJournal.log(grid, com.simpleae.bytestorage.util.JournalLog.STOCK_ORDER,
                        wanted, stockMin[i] - current);
                continue;
            }
            stockReason[i] = com.simpleae.bytestorage.util.CraftingService.diagnose(grid, wanted, stockMin[i] - current);
            if (previous != STOCK_FAILED) {
                com.simpleae.bytestorage.util.NetworkJournal.log(grid, com.simpleae.bytestorage.util.JournalLog.STOCK_FAILED,
                        wanted, stockMin[i] - current);
            }
            if (++stockFails[i] >= ALERT_AFTER_FAILS && stockReason[i] != null) {
                alert("stock:" + i, net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.alert.stock",
                        wanted.getHoverName(), stockReason[i]));
            }
        }
    }

    /** Пропускать «распаковку» (блок -> много предметов), если есть обратный рецепт. */
    private boolean skipDecompress = true;
    /** Расписание правил запаса (см. StockSchedule) и признак «сейчас работают». */
    private final com.simpleae.bytestorage.util.StatsHistory history = new com.simpleae.bytestorage.util.StatsHistory();

    public com.simpleae.bytestorage.util.StatsHistory getHistory() {
        return history;
    }

    private void sampleHistory(IGrid grid) {
        com.simpleae.bytestorage.api.storage.INetworkStorage storage = grid.getStorage();
        if (storage == null) {
            return;
        }
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (ItemStack stack : storage.getAllItems()) {
            counts.merge(com.simpleae.bytestorage.util.CraftingService.key(stack), stack.getCount(), Integer::sum);
        }
        int percent = energyStorage.getMaxEnergyStored() <= 0 ? 0
                : (int) (energyStorage.getEnergyStored() * 100L / energyStorage.getMaxEnergyStored());
        history.record(counts, percent);
    }

    private int stockMode = com.simpleae.bytestorage.util.StockSchedule.ALWAYS;
    private boolean stockActive = true;

    public int getStockMode() {
        return stockMode;
    }

    public boolean isStockScheduleActive() {
        return stockActive;
    }

    public void setStockMode(int mode) {
        int clamped = Math.max(0, Math.min(com.simpleae.bytestorage.util.StockSchedule.MODES - 1, mode));
        if (clamped != stockMode) {
            stockMode = clamped;
            stockActive = true;
            setChanged();
        }
    }

    public void cycleStockMode() {
        setStockMode((stockMode + 1) % com.simpleae.bytestorage.util.StockSchedule.MODES);
    }

    private IGrid grid;
    /** Резервы, прочитанные из мира; применяются при первом тике, когда сеть уже собрана. */
    private java.util.Map<String, java.util.Map<String, Integer>> pendingReservations;
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

    public ItemStackHandler getRecipeBlacklist() {
        return recipeBlacklist;
    }

    public ItemStackHandler getRecipePrefs() {
        return recipePrefs;
    }

    public boolean isSkipDecompress() {
        return skipDecompress;
    }

    public void setSkipDecompress(boolean value) {
        this.skipDecompress = value;
        setChanged();
        com.simpleae.bytestorage.util.RecipeLibrary.invalidate();
    }

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

    /**
     * Отдать энергию сети в подключённые машины (Шина Шаблонов, Энергоинтерфейс). Всегда оставляет запас на
     * собственное потребление сети, чтобы машины не «обесточили» сеть.
     */
    public int drawSurplusEnergy(int amount) {
        int drain = grid == null ? 0 : grid.getTotalEnergyDrain();
        int reserve = Math.max(500, drain * 40);
        int available = energyStorage.getEnergyStored() - reserve;
        if (available <= 0 || amount <= 0) {
            return 0;
        }
        return energyStorage.consumeInternalEnergy(Math.min(amount, available));
    }

    /** Вернуть в буфер энергию, которую машина не приняла. */
    public void refundEnergy(int amount) {
        if (amount > 0) {
            energyStorage.setEnergy(energyStorage.getEnergyStored() + amount);
        }
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

        if (controller.pendingReservations != null && grid.getStorage() != null) {
            grid.getStorage().restoreReservations(controller.pendingReservations);
            controller.pendingReservations = null;
        }
        if (controller.tickCounter % 60 == 15 && grid.getState().isActive() && grid.getStorage() != null
                && grid.getStorage().getTotalReserved() > 0
                && com.simpleae.bytestorage.util.CraftingService.queue(grid).isEmpty()) {
            // очередь пуста — резервы больше ни за чем не стоят (заказы завершены, отменены или потеряны)
            grid.getStorage().clearReservations();
        }
        if (controller.tickCounter % 60 == 45 && grid.getState().isActive()) {
            controller.checkStuck(grid);
        }
        if (controller.tickCounter % 60 == 30 && grid.getState().isActive()) {
            controller.runStockRules(grid);
        }
        if (controller.tickCounter % com.simpleae.bytestorage.util.StatsHistory.SAMPLE_TICKS == 5 && grid.getState().isActive()) {
            controller.sampleHistory(grid);
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
        tag.put("RecipeBlacklist", recipeBlacklist.serializeNBT());
        tag.put("RecipePrefs", recipePrefs.serializeNBT());
        tag.putBoolean("SkipDecompress", skipDecompress);
        tag.putInt("StockMode", stockMode);
        tag.put("StockItems", stockItems.serializeNBT());
        tag.putIntArray("StockMin", stockMin);
        CompoundTag prio = new CompoundTag();
        queuePriority.forEach(prio::putInt);
        tag.put("QueuePriority", prio);
        net.minecraft.nbt.ListTag paused = new net.minecraft.nbt.ListTag();
        for (String id : queuePaused) {
            paused.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        tag.put("QueuePaused", paused);
        // Резерви сировини під замовлення — переживають перезавантаження світу
        java.util.Map<String, java.util.Map<String, Integer>> reservations = pendingReservations;
        if (reservations == null && grid != null && grid.getStorage() != null) {
            reservations = grid.getStorage().getReservationSnapshot();
        }
        if (reservations != null && !reservations.isEmpty()) {
            CompoundTag res = new CompoundTag();
            reservations.forEach((order, items) -> {
                CompoundTag one = new CompoundTag();
                items.forEach(one::putInt);
                res.put(order, one);
            });
            tag.put("Reservations", res);
        }
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
        if (tag.contains("RecipeBlacklist")) {
            recipeBlacklist.deserializeNBT(tag.getCompound("RecipeBlacklist"));
        }
        if (tag.contains("RecipePrefs")) {
            recipePrefs.deserializeNBT(tag.getCompound("RecipePrefs"));
        }
        if (tag.contains("StockItems")) {
            stockItems.deserializeNBT(tag.getCompound("StockItems"));
        }
        int[] mins = tag.getIntArray("StockMin");
        for (int i = 0; i < Math.min(mins.length, STOCK_RULES); i++) {
            stockMin[i] = mins[i];
        }
        queuePriority.clear();
        queuePaused.clear();
        CompoundTag prio = tag.getCompound("QueuePriority");
        for (String k : prio.getAllKeys()) {
            queuePriority.put(k, prio.getInt(k));
        }
        for (Tag t : tag.getList("QueuePaused", Tag.TAG_STRING)) {
            queuePaused.add(t.getAsString());
        }
        pendingReservations = null;
        if (tag.contains("Reservations", Tag.TAG_COMPOUND)) {
            java.util.Map<String, java.util.Map<String, Integer>> data = new java.util.LinkedHashMap<>();
            CompoundTag res = tag.getCompound("Reservations");
            for (String order : res.getAllKeys()) {
                CompoundTag one = res.getCompound(order);
                java.util.Map<String, Integer> items = new java.util.HashMap<>();
                for (String k : one.getAllKeys()) {
                    items.put(k, one.getInt(k));
                }
                data.put(order, items);
            }
            pendingReservations = data;
        }
        stockMode = Math.max(0, Math.min(com.simpleae.bytestorage.util.StockSchedule.MODES - 1, tag.getInt("StockMode")));
        stockActive = true;
        if (tag.contains("SkipDecompress")) {
            skipDecompress = tag.getBoolean("SkipDecompress");
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
