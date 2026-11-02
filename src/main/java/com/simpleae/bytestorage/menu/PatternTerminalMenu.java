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

    // Клієнтські дані
    private final List<PatternData> patterns = new ArrayList<>();
    private final Set<String> craftable = new HashSet<>();
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
    }

    private void sync(ServerPlayer serverPlayer) {
        IGrid grid = terminal.getGrid();
        List<PatternData> list = new ArrayList<>();
        List<Boolean> flags = new ArrayList<>();
        int used = 0;
        int capacity = 0;
        int assemblers = 0;

        if (grid != null && grid.getState().isActive() && grid.getPatternStorage() != null) {
            IPatternStorage storage = grid.getPatternStorage();
            for (PatternData pattern : storage.getAllPatterns()) {
                list.add(pattern);
                flags.add(storage.canCraft(grid, pattern.getId()));
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
                            flags.add(PatternStorage.hasInputs(grid, pattern));
                        }
                    }
                }
            }
        }

        int signature = 17;
        for (int i = 0; i < list.size(); i++) {
            signature = signature * 31 + list.get(i).getId().hashCode();
            signature = signature * 31 + (flags.get(i) ? 1 : 0);
        }
        signature = signature * 31 + used;
        signature = signature * 31 + capacity;
        signature = signature * 31 + assemblers;

        if (forceSync || signature != lastSignature || keepAliveTimer >= 60) {
            lastSignature = signature;
            keepAliveTimer = 0;
            forceSync = false;
            SimpleByteStoragePackets.sendToPlayer(
                    new SyncPatternDataPacket(used, capacity, assemblers, list, flags), serverPlayer);
        }
    }

    // --- Дії гравця (сервер) ---

    public void handleAction(ServerPlayer serverPlayer, int action, String patternId, int amount) {
        if (terminal == null) {
            return;
        }
        if (action == PatternOrderPacket.ACTION_ORDER) {
            order(serverPlayer, patternId, amount);
        } else if (action == PatternOrderPacket.ACTION_REMOVE) {
            IGrid grid = terminal.getGrid();
            if (grid != null && grid.getPatternStorage() != null && grid.getPatternStorage().removePattern(patternId)) {
                serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.removed"), true);
                forceSync = true;
            }
        }
    }

    private void order(ServerPlayer serverPlayer, String patternId, int amount) {
        IGrid grid = terminal.getGrid();
        if (grid == null || !grid.getState().isActive() || grid.getPatternStorage() == null) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_network"), true);
            return;
        }
        PatternData pattern = grid.getPatternStorage().getPattern(patternId);
        if (pattern == null) {
            orderProcessing(serverPlayer, grid, patternId, amount);
            return;
        }

        List<AssemblerBlockEntity> ready = new ArrayList<>();
        boolean anyAssembler = false;
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof AssemblerBlockEntity assembler) {
                anyAssembler = true;
                if (assembler.hasBufferCell()) {
                    ready.add(assembler);
                }
            }
        }
        if (!anyAssembler) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_assembler"), true);
            return;
        }
        if (ready.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_card"), true);
            return;
        }

        int total = Math.max(1, Math.min(MAX_ORDER, amount));
        Map<AssemblerBlockEntity, Integer> share = new HashMap<>();
        for (int i = 0; i < total; i++) {
            AssemblerBlockEntity best = null;
            int bestLoad = Integer.MAX_VALUE;
            for (AssemblerBlockEntity assembler : ready) {
                int load = assembler.getPendingCrafts() + share.getOrDefault(assembler, 0);
                if (load < bestLoad) {
                    bestLoad = load;
                    best = assembler;
                }
            }
            share.merge(best, 1, Integer::sum);
        }

        int accepted = 0;
        for (Map.Entry<AssemblerBlockEntity, Integer> entry : share.entrySet()) {
            accepted += entry.getKey().enqueue(patternId, entry.getValue());
        }

        if (accepted <= 0) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.queue_full"), true);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.ok",
                    accepted, pattern.getOutput().getHoverName()), true);
        }
    }

    /** Замовлення процесного шаблону: ділимо крафти між усіма шинами, які його містять. */
    private void orderProcessing(ServerPlayer serverPlayer, IGrid grid, String patternId, int amount) {
        List<PatternProviderBlockEntity> holders = new ArrayList<>();
        PatternData pattern = null;
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof PatternProviderBlockEntity provider) {
                PatternData found = provider.findPattern(patternId);
                if (found != null) {
                    holders.add(provider);
                    pattern = found;
                }
            }
        }
        if (pattern == null) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_pattern"), true);
            return;
        }
        int total = Math.max(1, Math.min(MAX_ORDER, amount));
        int accepted = 0;
        for (int i = 0; i < total; i++) {
            PatternProviderBlockEntity best = null;
            int bestLoad = Integer.MAX_VALUE;
            for (PatternProviderBlockEntity provider : holders) {
                if (provider.getPendingCrafts() < bestLoad) {
                    bestLoad = provider.getPendingCrafts();
                    best = provider;
                }
            }
            if (best == null || best.enqueue(patternId, 1) <= 0) {
                break;
            }
            accepted++;
        }
        if (accepted <= 0) {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.queue_full"), true);
        } else {
            serverPlayer.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.ok",
                    accepted, pattern.getOutput().getHoverName()), true);
        }
    }

    // --- Клієнтські дані ---

    public void updateClientData(int used, int capacity, int assemblers,
                                 List<PatternData> newPatterns, List<Boolean> flags) {
        this.usedBytes = used;
        this.capacityBytes = capacity;
        this.assemblerCount = assemblers;
        this.patterns.clear();
        this.patterns.addAll(newPatterns);
        this.craftable.clear();
        for (int i = 0; i < newPatterns.size() && i < flags.size(); i++) {
            if (flags.get(i)) {
                this.craftable.add(newPatterns.get(i).getId());
            }
        }
    }

    public List<PatternData> getPatterns() {
        return patterns;
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
