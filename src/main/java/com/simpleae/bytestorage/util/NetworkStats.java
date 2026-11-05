package com.simpleae.bytestorage.util;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.ICellInventory;
import com.simpleae.bytestorage.api.storage.IDriveHost;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.block.entity.AssemblerBlockEntity;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.block.entity.PatternProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Зведена статистика мережі для екрана Контролера. Збирається на сервері, передається клієнту пакетом.
 */
public final class NetworkStats {

    public static final int TOP_ITEMS = 8;
    public static final int MAX_QUEUE_ROWS = 8;

    /** Скільки блоків певного виду підключено (key — ключ перекладу назви блока). */
    public record BlockCount(String nameKey, int count) {
    }

    public record DriveInfo(BlockPos pos, int used, int max, int cells) {
    }

    /** Предмет мережі (stack — один екземпляр для іконки) і сумарна кількість. */
    public record ItemCount(ItemStack stack, int count) {
    }

    public record QueueRow(ItemStack output, int crafts, boolean active, int machines, String patternId, int priority, boolean paused, int etaSeconds, int percent) {
    }

    public int state;
    public int energy;
    public int maxEnergy;
    public int drain;
    public int usedBytes;
    public int capacityBytes;
    public int itemTypes;
    public int nodeTotal;
    public int patterns;
    public int assemblers;
    public int assemblersBusy;
    public int assemblerQueue;
    public int buses;
    public int busJobs;
    public int libraryRecipes; // 0 — Библиотека рецептов не установлена

    public final int[] stockMin = new int[NetworkControllerBlockEntity.STOCK_RULES];
    public final int[] stockHave = new int[NetworkControllerBlockEntity.STOCK_RULES];
    public final int[] stockState = new int[NetworkControllerBlockEntity.STOCK_RULES];

    public final List<BlockCount> blocks = new ArrayList<>();
    public final List<DriveInfo> drives = new ArrayList<>();
    public final List<ItemCount> topItems = new ArrayList<>();
    public final List<QueueRow> queue = new ArrayList<>();
    public static final int JOURNAL_ROWS = 30;
    public record JournalRow(int type, ItemStack item, int amount, int ageSeconds) {
    }
    public final List<JournalRow> journal = new ArrayList<>();

    // --- Збір на сервері ---

    public static NetworkStats collect(NetworkControllerBlockEntity controller) {
        NetworkStats s = new NetworkStats();
        IGrid grid = controller.getGrid();
        s.energy = controller.getEnergyStored();
        s.maxEnergy = controller.getMaxEnergyStored();
        if (grid == null) {
            return s;
        }
        s.state = grid.getState().ordinal();
        s.drain = grid.getTotalEnergyDrain();
        s.nodeTotal = grid.getNodes().size();

        Map<String, Integer> counts = new HashMap<>();
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof BlockEntity be) {
                counts.merge(be.getBlockState().getBlock().getDescriptionId(), 1, Integer::sum);
            }
            if (node instanceof IDriveHost host) {
                int used = 0;
                int max = 0;
                int cells = 0;
                for (ICellInventory cell : host.getActiveCellInventories()) {
                    used += cell.getUsedBytes();
                    max += cell.getMaxBytes();
                    cells++;
                }
                s.drives.add(new DriveInfo(node.getNodePos(), used, max, cells));
            }
            if (node instanceof AssemblerBlockEntity assembler) {
                s.assemblers++;
                int pending = assembler.getPendingCrafts();
                s.assemblerQueue += pending;
                if (pending > 0) {
                    s.assemblersBusy++;
                }
            }
            if (node instanceof PatternProviderBlockEntity bus) {
                s.buses++;
                s.busJobs += bus.getPendingCrafts();
            }
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            s.blocks.add(new BlockCount(e.getKey(), e.getValue()));
        }
        s.blocks.sort(Comparator.comparingInt(BlockCount::count).reversed().thenComparing(BlockCount::nameKey));
        s.drives.sort(Comparator.comparingLong(d -> d.pos().asLong()));

        INetworkStorage storage = grid.getStorage();
        if (storage != null) {
            s.usedBytes = storage.getTotalUsedBytes();
            s.capacityBytes = storage.getTotalCapacityBytes();
            List<ItemStack> all = storage.getAllItems();
            s.itemTypes = all.size();
            Map<Item, Integer> perItem = new HashMap<>();
            for (ItemStack stack : all) {
                perItem.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
            List<Map.Entry<Item, Integer>> sorted = new ArrayList<>(perItem.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            for (int i = 0; i < Math.min(TOP_ITEMS, sorted.size()); i++) {
                s.topItems.add(new ItemCount(new ItemStack(sorted.get(i).getKey()), sorted.get(i).getValue()));
            }
        }

        for (int i = 0; i < NetworkControllerBlockEntity.STOCK_RULES; i++) {
            s.stockMin[i] = controller.getStockMin(i);
            s.stockHave[i] = controller.getStockHave(i);
            s.stockState[i] = controller.getStockState(i);
        }
        if (controller.getLevel() != null) {
            long now = controller.getLevel().getGameTime();
            for (JournalLog.Entry e : controller.getJournal().latest(JOURNAL_ROWS)) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(e.item()));
                if (item != null) {
                    s.journal.add(new JournalRow(e.type(), new ItemStack(item), e.amount(), (int) Math.max(0, (now - e.tick()) / 20)));
                }
            }
        }
        s.patterns = CraftingService.allPatterns(grid).size();
        s.libraryRecipes = RecipeLibrary.count(grid);
        for (CraftingService.QueueEntry entry : CraftingService.queue(grid)) {
            if (s.queue.size() >= MAX_QUEUE_ROWS) {
                break;
            }
            ItemStack out = entry.pattern.getOutput().copy();
            out.setCount(1);
            int percent = entry.ordered <= 0 ? 0 : Math.max(0, Math.min(100, 100 - entry.crafts * 100 / entry.ordered));
            s.queue.add(new QueueRow(out, entry.crafts, entry.active, entry.machines, entry.pattern.getId(), entry.priority,
                    entry.paused, (entry.etaTicks + 19) / 20, percent));
        }
        return s;
    }

    // --- Мережа ---

    private static void writeItem(FriendlyByteBuf buf, ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        buf.writeResourceLocation(key == null ? new ResourceLocation("minecraft:air") : key);
    }

    private static ItemStack readItem(FriendlyByteBuf buf) {
        Item item = ForgeRegistries.ITEMS.getValue(buf.readResourceLocation());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(state);
        buf.writeVarInt(energy);
        buf.writeVarInt(maxEnergy);
        buf.writeVarInt(drain);
        buf.writeVarInt(usedBytes);
        buf.writeVarInt(capacityBytes);
        buf.writeVarInt(itemTypes);
        buf.writeVarInt(nodeTotal);
        buf.writeVarInt(patterns);
        buf.writeVarInt(assemblers);
        buf.writeVarInt(assemblersBusy);
        buf.writeVarInt(assemblerQueue);
        buf.writeVarInt(buses);
        buf.writeVarInt(busJobs);
        buf.writeVarInt(libraryRecipes);
        for (int i = 0; i < stockMin.length; i++) {
            buf.writeVarInt(stockMin[i]);
            buf.writeVarInt(stockHave[i]);
            buf.writeVarInt(stockState[i]);
        }
        buf.writeVarInt(blocks.size());
        for (BlockCount b : blocks) {
            buf.writeUtf(b.nameKey(), 128);
            buf.writeVarInt(b.count());
        }
        buf.writeVarInt(drives.size());
        for (DriveInfo d : drives) {
            buf.writeBlockPos(d.pos());
            buf.writeVarInt(d.used());
            buf.writeVarInt(d.max());
            buf.writeVarInt(d.cells());
        }
        buf.writeVarInt(topItems.size());
        for (ItemCount i : topItems) {
            writeItem(buf, i.stack());
            buf.writeVarInt(i.count());
        }
        buf.writeVarInt(queue.size());
        for (QueueRow q : queue) {
            writeItem(buf, q.output());
            buf.writeVarInt(q.crafts());
            buf.writeBoolean(q.active());
            buf.writeVarInt(q.machines());
            buf.writeUtf(q.patternId());
            buf.writeVarInt(q.priority() + 16);
            buf.writeBoolean(q.paused());
            buf.writeVarInt(q.etaSeconds());
            buf.writeVarInt(q.percent());
        }
        buf.writeVarInt(journal.size());
        for (JournalRow j : journal) {
            buf.writeVarInt(j.type());
            writeItem(buf, j.item());
            buf.writeVarInt(j.amount());
            buf.writeVarInt(j.ageSeconds());
        }
    }

    public static NetworkStats read(FriendlyByteBuf buf) {
        NetworkStats s = new NetworkStats();
        s.state = buf.readVarInt();
        s.energy = buf.readVarInt();
        s.maxEnergy = buf.readVarInt();
        s.drain = buf.readVarInt();
        s.usedBytes = buf.readVarInt();
        s.capacityBytes = buf.readVarInt();
        s.itemTypes = buf.readVarInt();
        s.nodeTotal = buf.readVarInt();
        s.patterns = buf.readVarInt();
        s.assemblers = buf.readVarInt();
        s.assemblersBusy = buf.readVarInt();
        s.assemblerQueue = buf.readVarInt();
        s.buses = buf.readVarInt();
        s.busJobs = buf.readVarInt();
        s.libraryRecipes = buf.readVarInt();
        for (int i = 0; i < s.stockMin.length; i++) {
            s.stockMin[i] = buf.readVarInt();
            s.stockHave[i] = buf.readVarInt();
            s.stockState[i] = buf.readVarInt();
        }
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            s.blocks.add(new BlockCount(buf.readUtf(128), buf.readVarInt()));
        }
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            s.drives.add(new DriveInfo(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            s.topItems.add(new ItemCount(readItem(buf), buf.readVarInt()));
        }
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            s.queue.add(new QueueRow(readItem(buf), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(),
                    buf.readUtf(), buf.readVarInt() - 16, buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));
        }
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            s.journal.add(new JournalRow(buf.readVarInt(), readItem(buf), buf.readVarInt(), buf.readVarInt()));
        }
        return s;
    }
}
