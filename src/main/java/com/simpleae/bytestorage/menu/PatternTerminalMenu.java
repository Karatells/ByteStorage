package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.block.entity.AssemblerBlockEntity;
import com.simpleae.bytestorage.block.entity.PatternProviderBlockEntity;
import com.simpleae.bytestorage.block.entity.PatternTerminalBlockEntity;
import com.simpleae.bytestorage.storage.PatternStorage;
import com.simpleae.bytestorage.util.CraftingService;
import com.simpleae.bytestorage.network.packet.CraftPlanPacket;
import com.simpleae.bytestorage.network.packet.PatternOrderPacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.network.packet.SyncPatternDataPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Меню глобального Терміналу Шаблонів.
 *
 * Сервер сам розсилає клієнту актуальний список шаблонів (при відкритті, при зміні списку,
 * при зміні запасів мережі). Клієнт надсилає лише дії: замовити крафт / видалити шаблон.
 */
public class PatternTerminalMenu extends AbstractContainerMenu {

    public static final int MAX_ORDER = 1024;

    // Серверна частина (на клієнті terminal == null)
    private final PatternTerminalBlockEntity terminal;
    private final Player player;
    private int syncTimer = 0;
    private int keepAliveTimer = 0;
    private int lastSignature = 0;
    private boolean forceSync = true;
    private int libTimer = 0;
    private int libSignature = 0;
    private boolean libForce = true;

    // Клієнтські дані
    private final List<PatternData> patterns = new ArrayList<>();
    private final Set<String> craftable = new HashSet<>();
    private final Map<String, Integer> missingMasks = new HashMap<>();
    private int usedBytes;
    private int capacityBytes;
    private int assemblerCount;

    public PatternTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, (PatternTerminalBlockEntity) null);
    }

    public PatternTerminalMenu(int containerId, Inventory playerInventory, PatternTerminalBlockEntity terminal) {
        super(SimpleByteStorage.PATTERN_TERMINAL_MENU.get(), containerId);
        this.terminal = terminal;
        this.player = playerInventory.player;
    }

    @Override
    public boolean stillValid(Player player) {
        if (terminal == null) {
            return true;
        }
        Level level = terminal.getNodeLevel();
        return level != null
                && level.getBlockState(terminal.getNodePos()).is(SimpleByteStorage.PATTERN_TERMINAL_BLOCK.get())
                && player.distanceToSqr(terminal.getNodePos().getX() + 0.5, terminal.getNodePos().getY() + 0.5,
                terminal.getNodePos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    // --- Серверна синхронізація ---

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (terminal == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        keepAliveTimer++;
        if (!forceSync && --syncTimer > 0) {
            return;
        }
        syncTimer = 10;
        sync(serverPlayer);
        syncLibrary(serverPlayer);
    }

    /** Рецепты Библиотеки рецептов (если установлена): компактный пакет, не чаще раза в 2 секунды. */
    private void syncLibrary(ServerPlayer serverPlayer) {
        if (!libForce && ++libTimer < 4) {
            return;
        }
        libTimer = 0;
        IGrid grid = terminal.getGrid();
        List<PatternData> lib = new ArrayList<>();
        List<Integer> masks = new ArrayList<>();
        if (grid != null && grid.getState().isActive() && grid.getStorage() != null) {
            for (PatternData pattern : com.simpleae.bytestorage.util.RecipeLibrary.patterns(grid)) {
                lib.add(pattern);
                masks.add(PatternStorage.missingMask(grid, pattern));
            }
        }
        int signature = lib.size();
        for (int i = 0; i < lib.size(); i++) {
            signature = signature * 31 + lib.get(i).getId().hashCode();
            signature = signature * 31 + masks.get(i);
        }
        if (libForce || signature != libSignature) {
            libForce = false;
            libSignature = signature;
            SimpleByteStoragePackets.sendToPlayer(new com.simpleae.bytestorage.network.packet.SyncLibraryPacket(lib, masks), serverPlayer);
        }
    }

    private void sync(ServerPlayer serverPlayer) {
        IGrid grid = terminal.getGrid();
        List<PatternData> list = new ArrayList<>();
        List<Integer> flags = new ArrayList<>();
        int used = 0;
        int capacity = 0;
        int assemblers = 0;

        if (grid != null && grid.getState().isActive() && grid.getPatternStorage() != null) {
            IPatternStorage storage = grid.getPatternStorage();
            for (PatternData pattern : storage.getAllPatterns()) {
                list.add(pattern);
                flags.add(PatternStorage.missingMask(grid, pattern));
            }
            INetworkStorage networkStorage = grid.getStorage();
            if (networkStorage != null) {
                used = networkStorage.getTotalUsedBytes();
                capacity = networkStorage.getTotalCapacityBytes();
            }
            for (IGridNode node : grid.getNodes()) {
                if (node instanceof AssemblerBlockEntity) {
                    assemblers++;
                } else if (node instanceof PatternProviderBlockEntity provider) {
                    // Процесні шаблони з Шин Шаблонів теж показуємо в списку
                    for (PatternData pattern : provider.getPatterns()) {
                        boolean known = false;
                        for (PatternData existing : list) {
                            known |= existing.getId().equals(pattern.getId());
                        }
                        if (!known) {
                            list.add(pattern);
                            flags.add(PatternStorage.missingMask(grid, pattern));
                        }
                    }
                }
            }
        }

        List<String> queueIds = new ArrayList<>();
        List<CraftPlanPacket.Entry> queue = new ArrayList<>();
        if (grid != null && grid.getState().isActive()) {
            for (CraftingService.QueueEntry entry : CraftingService.queue(grid)) {
                queueIds.add(entry.pattern.getId());
                queue.add(new CraftPlanPacket.Entry(entry.pattern.getOutput(), entry.crafts, entry.active));
            }
        }

        int signature = 17;
        for (int i = 0; i < queue.size(); i++) {
            signature = signature * 31 + queueIds.get(i).hashCode();
            signature = signature * 31 + queue.get(i).count;
            signature = signature * 31 + (queue.get(i).processing ? 1 : 0);
        }
        for (int i = 0; i < list.size(); i++) {
            signature = signature * 31 + list.get(i).getId().hashCode();
            signature = signature * 31 + flags.get(i);
        }
        signature = signature * 31 + used;
        signature = signature * 31 + capacity;
        signature = signature * 31 + assemblers;

        if (forceSync || signature != lastSignature || keepAliveTimer >= 60) {
            lastSignature = signature;
            keepAliveTimer = 0;
            forceSync = false;
            SimpleByteStoragePackets.sendToPlayer(
                    new SyncPatternDataPacket(used, capacity, assemblers, list, flags, queueIds, queue), serverPlayer);
        }
    }

    // --- Дії гравця (сервер) ---

    public void handleAction(ServerPlayer serverPlayer, int action, String patternId, int amount) {
        if (terminal == null) {
            return;
        }
        if (action == PatternOrderPacket.ACTION_ORDER) {
            order(serverPlayer, patternId, amount);
        } else if (action == PatternOrderPacket.ACTION_CANCEL) {
            IGrid grid = terminal.getGrid();
            if (grid != null && grid.getState().isActive()) {
                CraftingService.cancel(grid, patternId);
                forceSync = true;
            }
        } else if (action == PatternOrderPacket.ACTION_PLAN) {
            sendPlan(serverPlayer, patternId, amount);
        } else if (action == PatternOrderPacket.ACTION_REMOVE) {
            IGrid grid = terminal.getGrid();
            if (grid != null && grid.getPatternStorage() != null && grid.getPatternStorage().removePattern(patternId)) {
                serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.removed"), true);
                forceSync = true;
            }
        }
    }

    private void sendPlan(ServerPlayer serverPlayer, String patternId, int amount) {
        IGrid grid = terminal.getGrid();
        if (grid == null || !grid.getState().isActive()) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_network"), true);
            return;
        }
        PatternData root = CraftingService.findPattern(grid, patternId);
        if (root == null) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_pattern"), true);
            return;
        }
        int total = Math.max(1, Math.min(MAX_ORDER, amount));
        CraftingService.Plan plan = CraftingService.plan(grid, root, total);
        SimpleByteStoragePackets.sendToPlayer(CraftPlanPacket.of(patternId, total, plan), serverPlayer);
    }

    private void order(ServerPlayer serverPlayer, String patternId, int amount) {
        IGrid grid = terminal.getGrid();
        if (grid == null || !grid.getState().isActive() || grid.getPatternStorage() == null) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_network"), true);
            return;
        }
        PatternData root = CraftingService.findPattern(grid, patternId);
        if (root == null) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_pattern"), true);
            return;
        }

        // План із підкрафтами: бракуючі проміжні предмети замовляються автоматично (якщо є їхній шаблон)
        CraftingService.Plan plan = CraftingService.plan(grid, root, Math.max(1, Math.min(MAX_ORDER, amount)));
        if (!plan.missing.isEmpty()) {
            ItemStack first = plan.missing.get(0);
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.missing",
                    first.getCount(), first.getHoverName()), true);
            return;
        }
        for (CraftingService.Step step : plan.steps) {
            String error = CraftingService.routeError(grid, step.pattern);
            if (error != null) {
                serverPlayer.displayClientMessage(Component.translatable(error), true);
                return;
            }
        }

        int rootAccepted = 0;
        for (CraftingService.Step step : plan.steps) {
            int accepted = CraftingService.enqueue(grid, step.pattern, step.crafts);
            if (step.pattern.getId().equals(root.getId())) {
                rootAccepted = accepted;
            }
        }

        if (rootAccepted <= 0) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.queue_full"), true);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.ok",
                    rootAccepted, root.getOutput().getHoverName()), true);
        }
    }

    // --- Клієнтські дані ---

    public void updateClientData(int used, int capacity, int assemblers,
                                 List<PatternData> newPatterns, List<Integer> flags) {
        this.usedBytes = used;
        this.capacityBytes = capacity;
        this.assemblerCount = assemblers;
        this.patterns.clear();
        this.patterns.addAll(newPatterns);
        this.craftable.clear();
        this.missingMasks.clear();
        for (int i = 0; i < newPatterns.size() && i < flags.size(); i++) {
            this.missingMasks.put(newPatterns.get(i).getId(), flags.get(i));
            if (flags.get(i) == 0) {
                this.craftable.add(newPatterns.get(i).getId());
            }
        }
        mergeLibrary();
    }

    // --- Библиотека рецептов (клиент) ---

    private List<PatternData> libPatterns = new ArrayList<>();
    private List<Integer> libMasks = new ArrayList<>();

    public void updateClientLibrary(List<PatternData> lib, List<Integer> masks) {
        this.libPatterns = lib;
        this.libMasks = masks;
        this.patterns.removeIf(p -> p.getId().startsWith(com.simpleae.bytestorage.util.RecipeLibrary.PREFIX));
        mergeLibrary();
    }

    private void mergeLibrary() {
        for (int i = 0; i < libPatterns.size(); i++) {
            PatternData p = libPatterns.get(i);
            this.patterns.add(p);
            int mask = i < libMasks.size() ? libMasks.get(i) : 0;
            this.missingMasks.put(p.getId(), mask);
            if (mask == 0) {
                this.craftable.add(p.getId());
            } else {
                this.craftable.remove(p.getId());
            }
        }
    }

    // --- План замовлення (клієнт) ---

    private String planPatternId;
    private int planAmount;
    private List<CraftPlanPacket.Entry> planSteps = new ArrayList<>();
    private List<CraftPlanPacket.Entry> planMissing = new ArrayList<>();

    public void setClientPlan(String patternId, int amount,
                              List<CraftPlanPacket.Entry> steps, List<CraftPlanPacket.Entry> missing) {
        this.planPatternId = patternId;
        this.planAmount = amount;
        this.planSteps = steps;
        this.planMissing = missing;
    }

    public void clearClientPlan() {
        this.planPatternId = null;
    }

    public boolean hasClientPlan() {
        return planPatternId != null;
    }

    public String getPlanPatternId() {
        return planPatternId;
    }

    public int getPlanAmount() {
        return planAmount;
    }

    public List<CraftPlanPacket.Entry> getPlanSteps() {
        return planSteps;
    }

    public List<CraftPlanPacket.Entry> getPlanMissing() {
        return planMissing;
    }

    // --- Черга (клієнт) ---

    private List<String> queueIds = new ArrayList<>();
    private List<CraftPlanPacket.Entry> queueEntries = new ArrayList<>();

    public void updateClientQueue(List<String> ids, List<CraftPlanPacket.Entry> entries) {
        this.queueIds = ids;
        this.queueEntries = entries;
    }

    /** Рядок черги: stack = результат, count = крафтів лишилось, processing = зараз виконується. */
    public List<CraftPlanPacket.Entry> getQueueEntries() {
        return queueEntries;
    }

    public String getQueueId(int index) {
        return queueIds.get(index);
    }

    public List<PatternData> getPatterns() {
        return patterns;
    }

    /** Маска бракуючих інгредієнтів шаблону (біт i = i-й вхід). */
    public int getMissingMask(String id) {
        return missingMasks.getOrDefault(id, 0);
    }

    public boolean isCraftable(String id) {
        return craftable.contains(id);
    }

    public int getUsedBytes() {
        return usedBytes;
    }

    public int getCapacityBytes() {
        return capacityBytes;
    }

    public int getAssemblerCount() {
        return assemblerCount;
    }
}
