package com.simpleae.bytestorage.api;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.util.CraftingService;
import com.simpleae.bytestorage.util.JournalLog;
import com.simpleae.bytestorage.util.NetworkJournal;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Стабильный API управления сетью для аддонов (используется Simple Byte Storage: Scripting).
 * Все методы работают на сервере и безопасно возвращают пустые значения, если у сети нет контроллера.
 */
public final class NetworkControl {

    private NetworkControl() {
    }

    // --- Структуры ---

    public record QueueInfo(String id, ItemStack output, int crafts, boolean active, int priority, boolean paused,
                            int etaSeconds, int percent) {
    }

    public record StockInfo(int slot, ItemStack item, int minimum, int have, int state) {
    }

    public record JournalInfo(int type, String item, int amount, int ageSeconds, String text) {
    }

    public record StepInfo(String patternId, ItemStack output, int crafts) {
    }

    public record PlanInfo(boolean ok, List<StepInfo> steps, List<ItemStack> missing) {
    }

    public record EnergyInfo(int stored, int max) {
    }

    /** Состояние автокрафт-машины: {@code kind} — "assembler" или "bus", {@code wait} — код причины простоя. */
    public record MachineInfo(String kind, BlockPos pos, boolean busy, int pending, String waitReason, ItemStack output) {
    }

    private static final String[] ASSEMBLER_WAIT = {"none", "no_card", "resources", "card_busy", "card_small", "network_off", "storage_full"};
    private static final String[] BUS_WAIT = {"none", "resources", "no_machine"};

    /** Все Асемблеры и Шины шаблонов сети с их состоянием. */
    public static List<MachineInfo> machines(IGrid grid) {
        List<MachineInfo> result = new ArrayList<>();
        if (grid == null) {
            return result;
        }
        for (com.simpleae.bytestorage.api.network.IGridNode node : grid.getNodes()) {
            if (node instanceof com.simpleae.bytestorage.block.entity.AssemblerBlockEntity a) {
                int w = Math.max(0, Math.min(ASSEMBLER_WAIT.length - 1, a.getWaitReason()));
                result.add(new MachineInfo("assembler", a.getBlockPos(), a.isBusy(), a.getPendingCrafts(),
                        ASSEMBLER_WAIT[w], a.getCurrentOutput()));
            } else if (node instanceof com.simpleae.bytestorage.block.entity.PatternProviderBlockEntity b) {
                int w = Math.max(0, Math.min(BUS_WAIT.length - 1, b.getWaitReason()));
                result.add(new MachineInfo("bus", b.getBlockPos(), b.getPendingCrafts() > 0, b.getPendingCrafts(),
                        BUS_WAIT[w], b.getCurrentOutput()));
            }
        }
        result.sort(java.util.Comparator.comparingLong(m -> m.pos().asLong()));
        return result;
    }

    /** Сводная статистика сети (как на экране Контроллера). */
    @Nullable
    public static com.simpleae.bytestorage.util.NetworkStats stats(IGrid grid) {
        NetworkControllerBlockEntity c = controller(grid);
        return c == null ? null : com.simpleae.bytestorage.util.NetworkStats.collect(c);
    }

    @Nullable
    public static NetworkControllerBlockEntity controller(@Nullable IGrid grid) {
        return grid == null ? null : grid.getController();
    }

    // --- Очередь ---

    public static List<QueueInfo> queue(IGrid grid) {
        List<QueueInfo> result = new ArrayList<>();
        if (grid == null) {
            return result;
        }
        for (CraftingService.QueueEntry e : CraftingService.queue(grid)) {
            int percent = e.ordered <= 0 ? 0 : Math.max(0, Math.min(100, 100 - e.crafts * 100 / e.ordered));
            result.add(new QueueInfo(e.pattern.getId(), e.pattern.getOutput().copyWithCount(1), e.crafts, e.active,
                    e.priority, e.paused, (e.etaTicks + 19) / 20, percent));
        }
        return result;
    }

    public static boolean setPriority(IGrid grid, String patternId, int priority) {
        NetworkControllerBlockEntity c = controller(grid);
        if (c == null) {
            return false;
        }
        c.setQueuePriority(patternId, Math.max(-NetworkControllerBlockEntity.MAX_PRIORITY,
                Math.min(NetworkControllerBlockEntity.MAX_PRIORITY, priority)));
        return true;
    }

    public static boolean setPaused(IGrid grid, String patternId, boolean paused) {
        NetworkControllerBlockEntity c = controller(grid);
        if (c == null) {
            return false;
        }
        c.setQueuePaused(patternId, paused);
        return true;
    }

    public static boolean cancel(IGrid grid, String patternId) {
        if (grid == null || CraftingService.findPattern(grid, patternId) == null) {
            return false;
        }
        PatternData pattern = CraftingService.findPattern(grid, patternId);
        NetworkJournal.log(grid, JournalLog.CANCELLED, pattern.getOutput(), Math.max(1, pattern.getOutput().getCount()));
        CraftingService.cancel(grid, patternId);
        return true;
    }

    // --- Планирование ---

    public static boolean canCraft(IGrid grid, ItemStack output, int amount) {
        return grid != null && CraftingService.diagnoseOrder(grid, output, amount) == null;
    }

    @Nullable
    public static CraftingService.Diagnosis why(IGrid grid, ItemStack output, int amount) {
        return CraftingService.diagnoseOrder(grid, output, amount);
    }

    @Nullable
    public static PlanInfo plan(IGrid grid, ItemStack output, int amount) {
        PatternData found = grid == null ? null : CraftingService.findPatternForOutput(grid, output);
        if (found == null) {
            return null;
        }
        int perCraft = Math.max(1, found.getOutput().getCount());
        CraftingService.Plan plan = CraftingService.plan(grid, found, Math.max(1, (Math.max(1, amount) + perCraft - 1) / perCraft));
        List<StepInfo> steps = new ArrayList<>();
        for (CraftingService.Step s : plan.steps) {
            steps.add(new StepInfo(s.pattern.getId(), s.pattern.getOutput().copyWithCount(1), s.crafts));
        }
        List<ItemStack> missing = new ArrayList<>();
        for (ItemStack m : plan.missing) {
            missing.add(m.copy());
        }
        return new PlanInfo(plan.isOk(), steps, missing);
    }

    // --- Правила запаса ---

    /** Режим расписания правил запаса: {@code name} — always/night/day/energy, {@code active} — работают ли правила сейчас. */
    public record StockScheduleInfo(String name, boolean active) {
    }

    @Nullable
    public static StockScheduleInfo stockSchedule(IGrid grid) {
        NetworkControllerBlockEntity c = controller(grid);
        return c == null ? null : new StockScheduleInfo(com.simpleae.bytestorage.util.StockSchedule.name(c.getStockMode()),
                c.isStockScheduleActive());
    }

    public static boolean setStockSchedule(IGrid grid, String name) {
        NetworkControllerBlockEntity c = controller(grid);
        int mode = com.simpleae.bytestorage.util.StockSchedule.byName(name);
        if (c == null || mode < 0) {
            return false;
        }
        c.setStockMode(mode);
        return true;
    }

    public static List<StockInfo> stockRules(IGrid grid) {
        List<StockInfo> result = new ArrayList<>();
        NetworkControllerBlockEntity c = controller(grid);
        if (c == null) {
            return result;
        }
        for (int i = 0; i < NetworkControllerBlockEntity.STOCK_RULES; i++) {
            ItemStack item = c.getStockItems().getStackInSlot(i);
            if (!item.isEmpty() && c.getStockMin(i) > 0) {
                result.add(new StockInfo(i, item.copyWithCount(1), c.getStockMin(i), c.getStockHave(i), c.getStockState(i)));
            }
        }
        return result;
    }

    /** @return false, если номер правила неверен или нет контроллера */
    public static boolean setStockRule(IGrid grid, int rule, ItemStack item, int minimum) {
        NetworkControllerBlockEntity c = controller(grid);
        if (c == null || rule < 0 || rule >= NetworkControllerBlockEntity.STOCK_RULES) {
            return false;
        }
        c.setStockRule(rule, item, minimum);
        return true;
    }

    // --- Настройки рецептов ---

    public static List<ItemStack> recipeBlacklist(IGrid grid) {
        return stacks(controller(grid) == null ? null : controller(grid).getRecipeBlacklist());
    }

    public static List<ItemStack> recipePreferred(IGrid grid) {
        return stacks(controller(grid) == null ? null : controller(grid).getRecipePrefs());
    }

    /** Добавить/убрать предмет в чёрном списке; false — нет свободного слота (при добавлении) или нет контроллера. */
    public static boolean setBlacklisted(IGrid grid, ItemStack item, boolean listed) {
        NetworkControllerBlockEntity c = controller(grid);
        return c != null && toggleGhost(c.getRecipeBlacklist(), item, listed);
    }

    public static boolean setPreferred(IGrid grid, ItemStack item, boolean listed) {
        NetworkControllerBlockEntity c = controller(grid);
        return c != null && toggleGhost(c.getRecipePrefs(), item, listed);
    }

    public static boolean isSkipUnpacking(IGrid grid) {
        NetworkControllerBlockEntity c = controller(grid);
        return c != null && c.isSkipDecompress();
    }

    public static boolean setSkipUnpacking(IGrid grid, boolean value) {
        NetworkControllerBlockEntity c = controller(grid);
        if (c == null) {
            return false;
        }
        c.setSkipDecompress(value);
        return true;
    }

    private static List<ItemStack> stacks(@Nullable ItemStackHandler handler) {
        List<ItemStack> result = new ArrayList<>();
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    result.add(handler.getStackInSlot(i).copyWithCount(1));
                }
            }
        }
        return result;
    }

    private static boolean toggleGhost(ItemStackHandler handler, ItemStack item, boolean listed) {
        int existing = -1;
        int free = -1;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack in = handler.getStackInSlot(i);
            if (in.isEmpty()) {
                if (free < 0) {
                    free = i;
                }
            } else if (in.getItem() == item.getItem()) {
                existing = i;
            }
        }
        if (listed) {
            if (existing >= 0) {
                return true;
            }
            if (free < 0) {
                return false;
            }
            handler.setStackInSlot(free, item.copyWithCount(1));
            return true;
        }
        if (existing >= 0) {
            handler.setStackInSlot(existing, ItemStack.EMPTY);
        }
        return true;
    }

    // --- Журнал ---

    /** Запись скрипта в журнал контроллера. */
    public static boolean writeJournal(IGrid grid, String text, ItemStack icon, boolean warning) {
        NetworkControllerBlockEntity c = controller(grid);
        if (c == null || c.getLevel() == null || text == null || text.isBlank()) {
            return false;
        }
        c.getJournal().add(c.getLevel().getGameTime(), warning ? JournalLog.SCRIPT_WARN : JournalLog.SCRIPT,
                key(icon.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.PAPER) : icon), 1, text);
        return true;
    }

    /** Событие журнала для аддонов; {@code seq} растёт монотонно. */
    public record EventInfo(long seq, int type, String item, int amount, String text) {
    }

    /** Курсор «сейчас»: события, добавленные позже, вернёт {@link #eventsSince}. */
    public static long eventCursor(IGrid grid) {
        NetworkControllerBlockEntity c = controller(grid);
        return c == null ? 0 : c.getJournal().eventCount();
    }

    public static List<EventInfo> eventsSince(IGrid grid, long cursor) {
        List<EventInfo> result = new ArrayList<>();
        NetworkControllerBlockEntity c = controller(grid);
        if (c == null) {
            return result;
        }
        for (JournalLog.Event e : c.getJournal().eventsSince(cursor)) {
            result.add(new EventInfo(e.seq(), e.type(), e.item(), e.amount(), e.text()));
        }
        return result;
    }

    public static List<JournalInfo> readJournal(IGrid grid, int limit) {
        List<JournalInfo> result = new ArrayList<>();
        NetworkControllerBlockEntity c = controller(grid);
        if (c == null || c.getLevel() == null) {
            return result;
        }
        long now = c.getLevel().getGameTime();
        for (JournalLog.Entry e : c.getJournal().latest(Math.max(1, limit))) {
            result.add(new JournalInfo(e.type(), e.item(), e.amount(), (int) Math.max(0, (now - e.tick()) / 20), e.text()));
        }
        return result;
    }

    private static String key(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "minecraft:paper" : id.toString();
    }

    // --- Энергия, резерв, предупреждения ---

    @Nullable
    public static EnergyInfo energy(IGrid grid) {
        NetworkControllerBlockEntity c = controller(grid);
        return c == null ? null : new EnergyInfo(c.getEnergyStored(), c.getMaxEnergyStored());
    }

    public static int reserved(IGrid grid, ItemStack stack) {
        return grid == null || grid.getStorage() == null ? 0 : grid.getStorage().getReserved(stack);
    }

    public static int totalReserved(IGrid grid) {
        return grid == null || grid.getStorage() == null ? 0 : grid.getStorage().getTotalReserved();
    }

    /** Предупреждение в чат владельцам Беспроводных терминалов сети; false — слишком часто (раз в 30 с на источник). */
    public static boolean alert(IGrid grid, String sourceKey, String text) {
        NetworkControllerBlockEntity c = controller(grid);
        return c != null && c.scriptAlert(sourceKey, Component.literal(text));
    }
}
