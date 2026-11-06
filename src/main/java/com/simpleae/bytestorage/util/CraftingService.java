package com.simpleae.bytestorage.util;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.block.entity.AssemblerBlockEntity;
import com.simpleae.bytestorage.block.entity.PatternProviderBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Серверна логіка автокрафту: пошук шаблонів у мережі, планування ланцюжка підкрафтів
 * та розподіл замовлень між Асемблерами й Шинами Шаблонів.
 */
public final class CraftingService {

    public static final int MAX_DEPTH = 8;
    public static final int MAX_CRAFTS = 1024;

    private CraftingService() {
    }

    /** Один крок плану: скільки разів виконати шаблон. */
    public static final class Step {
        public final PatternData pattern;
        public int crafts;

        Step(PatternData pattern, int crafts) {
            this.pattern = pattern;
            this.crafts = crafts;
        }
    }

    /** План замовлення: кроки у порядку залежностей (спочатку підкрафти) і список того, чого бракує. */
    public static final class Plan {
        public final List<Step> steps = new ArrayList<>();
        public final List<ItemStack> missing = new ArrayList<>();
        /** Сировина, яку цей план бере з сховища (ключ предмета -> кількість): під неї замовлення бронює ресурси. */
        public final Map<String, Integer> reserve = new LinkedHashMap<>();
        private final Map<String, Integer> leftover = new HashMap<>();

        public boolean isOk() {
            return missing.isEmpty() && !steps.isEmpty();
        }
    }

    // --- Пошук шаблонів ---

    /** Усі шаблони, доступні мережі: глобальні + з Шин Шаблонів (без дублікатів за id). */
    public static List<PatternData> allPatterns(IGrid grid) {
        Map<String, PatternData> result = new LinkedHashMap<>();
        if (grid.getPatternStorage() != null) {
            for (PatternData pattern : grid.getPatternStorage().getAllPatterns()) {
                result.put(pattern.getId(), pattern);
            }
        }
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof PatternProviderBlockEntity provider) {
                for (PatternData pattern : provider.getPatterns()) {
                    result.putIfAbsent(pattern.getId(), pattern);
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    /** Явні шаблони + (якщо в Контролері встановлена Бібліотека рецептів) усі рецепти верстака гри. */
    public static List<PatternData> allPatternsWithLibrary(IGrid grid) {
        List<PatternData> result = allPatterns(grid);
        result.addAll(RecipeLibrary.patterns(grid));
        return result;
    }

    @Nullable
    public static PatternData findPattern(IGrid grid, String id) {
        if (grid.getPatternStorage() != null) {
            PatternData global = grid.getPatternStorage().getPattern(id);
            if (global != null) {
                return global;
            }
        }
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof PatternProviderBlockEntity provider) {
                PatternData found = provider.findPattern(id);
                if (found != null) {
                    return found;
                }
            }
        }
        return RecipeLibrary.find(grid, id);
    }

    public static String key(ItemStack stack) {
        net.minecraft.resources.ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String name = id != null ? id.toString() : "unknown";
        return stack.hasTag() ? name + stack.getTag() : name;
    }

    // --- Планування ---

    public static Plan plan(IGrid grid, PatternData root, int amount) {
        Plan plan = new Plan();
        INetworkStorage storage = grid.getStorage();
        if (storage == null || root == null) {
            return plan;
        }
        Map<String, Integer> available = new HashMap<>();
        for (ItemStack stack : storage.getAllItems()) {
            available.merge(key(stack), stack.getCount(), Integer::sum);
        }
        // Зарезервоване іншими замовленнями недоступне для нового плану
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ItemStack stack : storage.getAllItems()) {
            String k = key(stack);
            if (seen.add(k)) {
                int reserved = storage.getReserved(stack);
                if (reserved > 0) {
                    available.computeIfPresent(k, (key, have) -> Math.max(0, have - reserved));
                }
            }
        }
        Map<String, PatternData> producers = new HashMap<>();
        for (PatternData pattern : allPatternsWithLibrary(grid)) {
            producers.putIfAbsent(key(pattern.getOutput()), pattern);
        }

        Map<String, Step> steps = new LinkedHashMap<>();
        Deque<String> path = new ArrayDeque<>();
        int crafts = Math.max(1, Math.min(MAX_CRAFTS, amount));
        expand(root, crafts, available, producers, steps, plan.missing, path, plan);
        plan.steps.addAll(steps.values());
        return plan;
    }

    private static void expand(PatternData pattern, int crafts, Map<String, Integer> available,
                               Map<String, PatternData> producers, Map<String, Step> steps,
                               List<ItemStack> missing, Deque<String> path, Plan plan) {
        path.push(pattern.getId());
        for (ItemStack input : pattern.getInputs()) {
            require(input, (long) input.getCount() * crafts, available, producers, steps, missing, path, plan);
        }
        path.pop();
        Step existing = steps.get(pattern.getId());
        if (existing != null) {
            existing.crafts += crafts;
        } else {
            steps.put(pattern.getId(), new Step(pattern, crafts));
        }
    }

    private static void require(ItemStack template, long count, Map<String, Integer> available,
                                Map<String, PatternData> producers, Map<String, Step> steps,
                                List<ItemStack> missing, Deque<String> path, Plan plan) {
        String k = key(template);
        int have = available.getOrDefault(k, 0);
        long use = Math.min(have, count);
        available.put(k, (int) (have - use));
        if (use > 0) {
            // сначала тратятся излишки подкрафтов, и только остальное берётся из хранилища (его и бронируем)
            int fromLeftover = (int) Math.min(plan.leftover.getOrDefault(k, 0), use);
            plan.leftover.merge(k, -fromLeftover, Integer::sum);
            if (use - fromLeftover > 0) {
                plan.reserve.merge(k, (int) (use - fromLeftover), Integer::sum);
            }
        }
        long remaining = count - use;
        if (remaining <= 0) {
            return;
        }
        PatternData producer = producers.get(k);
        if (producer == null || path.contains(producer.getId()) || path.size() >= MAX_DEPTH) {
            addMissing(missing, template, remaining);
            return;
        }
        int perCraft = Math.max(1, producer.getOutput().getCount());
        int crafts = (int) Math.min(MAX_CRAFTS, (remaining + perCraft - 1) / perCraft);
        expand(producer, crafts, available, producers, steps, missing, path, plan);
        long leftover = (long) crafts * perCraft - remaining;
        if (leftover > 0) {
            available.merge(k, (int) Math.min(Integer.MAX_VALUE, leftover), Integer::sum);
            plan.leftover.merge(k, (int) Math.min(Integer.MAX_VALUE, leftover), Integer::sum);
        }
    }

    private static void addMissing(List<ItemStack> missing, ItemStack template, long count) {
        for (ItemStack entry : missing) {
            if (ItemStack.isSameItemSameTags(entry, template)) {
                entry.grow((int) Math.min(Integer.MAX_VALUE - entry.getCount(), count));
                return;
            }
        }
        ItemStack copy = template.copy();
        copy.setCount((int) Math.min(Integer.MAX_VALUE, count));
        missing.add(copy);
    }

    // --- Розподіл замовлень ---

    /** Ключ повідомлення про помилку, якщо крок не можна нікуди поставити; null — все гаразд. */
    @Nullable
    public static String routeError(IGrid grid, PatternData pattern) {
        if (pattern.isProcessing()) {
            for (IGridNode node : grid.getNodes()) {
                if (node instanceof PatternProviderBlockEntity provider && provider.findPattern(pattern.getId()) != null) {
                    return null;
                }
            }
            return "msg.simple_bytestorage.order.no_pattern";
        }
        boolean anyAssembler = false;
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof AssemblerBlockEntity assembler) {
                anyAssembler = true;
                if (assembler.hasBufferCell()) {
                    return null;
                }
            }
        }
        return anyAssembler ? "msg.simple_bytestorage.order.no_card" : "msg.simple_bytestorage.order.no_assembler";
    }

    /** Скільки крафтів було замовлено в сумі (для прогресу); очищається, коли шаблон зникає з черги. */
    private static final Map<IGrid, Map<String, Integer>> ORDERED = new java.util.WeakHashMap<>();

    private static synchronized void noteOrdered(IGrid grid, String id, int accepted) {
        if (accepted > 0) {
            ORDERED.computeIfAbsent(grid, g -> new HashMap<>()).merge(id, accepted, Integer::sum);
        }
    }

    /**
     * Який із завдань черги запускати наступним: найвищий пріоритет серед не призупинених (за однакового — раніший).
     * @return індекс або -1, якщо всі призупинені
     */
    public static int pickJob(IGrid grid, List<String> ids) {
        com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity c = grid == null ? null : grid.getController();
        if (c == null) {
            return ids.isEmpty() ? -1 : 0;
        }
        return QueueOrder.pick(ids, c::isQueuePaused, c::getQueuePriority);
    }

    /** Ставить {@code crafts} виконань шаблону в черги; повертає, скільки прийнято. */
    public static int enqueue(IGrid grid, PatternData pattern, int crafts) {
        int accepted = 0;
        if (pattern.isProcessing()) {
            List<PatternProviderBlockEntity> holders = new ArrayList<>();
            for (IGridNode node : grid.getNodes()) {
                if (node instanceof PatternProviderBlockEntity provider && provider.findPattern(pattern.getId()) != null) {
                    holders.add(provider);
                }
            }
            for (int i = 0; i < crafts; i++) {
                PatternProviderBlockEntity best = null;
                for (PatternProviderBlockEntity provider : holders) {
                    if (best == null || provider.getPendingCrafts() < best.getPendingCrafts()) {
                        best = provider;
                    }
                }
                if (best == null || best.enqueue(pattern.getId(), 1) <= 0) {
                    break;
                }
                accepted++;
            }
            noteOrdered(grid, pattern.getId(), accepted);
            NetworkJournal.log(grid, JournalLog.ORDER, pattern.getOutput(), accepted * Math.max(1, pattern.getOutput().getCount()));
            return accepted;
        }

        List<AssemblerBlockEntity> ready = new ArrayList<>();
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof AssemblerBlockEntity assembler && assembler.hasBufferCell()) {
                ready.add(assembler);
            }
        }
        Map<AssemblerBlockEntity, Integer> share = new HashMap<>();
        for (int i = 0; i < crafts && !ready.isEmpty(); i++) {
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
        for (Map.Entry<AssemblerBlockEntity, Integer> entry : share.entrySet()) {
            accepted += entry.getKey().enqueue(pattern.getId(), entry.getValue());
        }
        noteOrdered(grid, pattern.getId(), accepted);
        NetworkJournal.log(grid, JournalLog.ORDER, pattern.getOutput(), accepted * Math.max(1, pattern.getOutput().getCount()));
        return accepted;
    }

    /**
     * Почему заказ предмета сейчас невозможен (без постановки в очередь): сообщение или null, если заказ возможен.
     * Используется правилами запаса и предупреждениями.
     */
    @Nullable
    public static net.minecraft.network.chat.Component diagnose(IGrid grid, ItemStack output, int amount) {
        if (grid == null || !grid.getState().isActive()) {
            return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.no_network");
        }
        PatternData found = findPatternForOutput(grid, output);
        if (found == null) {
            return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.no_pattern");
        }
        int perCraft = Math.max(1, found.getOutput().getCount());
        int crafts = Math.max(1, Math.min(MAX_CRAFTS, (amount + perCraft - 1) / perCraft));
        Plan plan = plan(grid, found, crafts);
        if (!plan.missing.isEmpty()) {
            ItemStack first = plan.missing.get(0);
            return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.missing",
                    first.getCount(), first.getHoverName());
        }
        for (Step step : plan.steps) {
            String error = routeError(grid, step.pattern);
            if (error != null) {
                return net.minecraft.network.chat.Component.translatable(error);
            }
        }
        return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.queue_full");
    }

    /** Бронює сировину плана за замовленням {@code orderId} (id шаблона-цели); вызывать после успешной постановки в очередь. */
    public static void reservePlan(IGrid grid, Plan plan, String orderId) {
        if (grid != null && grid.getStorage() != null && !plan.reserve.isEmpty()) {
            grid.getStorage().reserve(orderId, plan.reserve);
        }
    }

    // --- Стабільний API для аддонів ---

    /**
     * Замовляє автокрафт предмета за поточним станом мережі.
     * Використовується аддонами (наприклад, Simple Byte Storage: Scripting).
     *
     * @return кількість виконань шаблону, прийнятих у чергу (0 — шаблону немає, не вистачає ресурсів або немає машин)
     */
    public static int requestCraft(IGrid grid, ItemStack output, int amount) {
        if (grid == null || output.isEmpty() || amount <= 0) {
            return 0;
        }
        PatternData found = null;
        String wanted = key(output);
        for (PatternData pattern : allPatternsWithLibrary(grid)) {
            if (key(pattern.getOutput()).equals(wanted)) {
                found = pattern;
                break;
            }
        }
        if (found == null || routeError(grid, found) != null) {
            return 0;
        }
        int perCraft = Math.max(1, found.getOutput().getCount());
        int crafts = (amount + perCraft - 1) / perCraft;
        Plan plan = plan(grid, found, crafts);
        if (!plan.isOk()) {
            return 0;
        }
        int accepted = 0;
        for (Step step : plan.steps) {
            int got = enqueue(grid, step.pattern, step.crafts);
            if (step.pattern == found || step.pattern.getId().equals(found.getId())) {
                accepted += got;
            }
        }
        if (accepted > 0) {
            reservePlan(grid, plan, found.getId());
        }
        return accepted;
    }

    // --- Черга мережі ---

    /** Рядок зведеної черги мережі. */
    public static final class QueueEntry {
        public final PatternData pattern;
        public int crafts;
        public boolean active;
        public int machines;
        public int etaTicks;
        public int ordered;
        public int priority;
        public boolean paused;

        QueueEntry(PatternData pattern) {
            this.pattern = pattern;
        }
    }

    /** Усі замовлення в Асемблерах і Шинах мережі, зведені за шаблонами. */
    public static List<QueueEntry> queue(IGrid grid) {
        Map<String, QueueEntry> result = new LinkedHashMap<>();
        for (IGridNode node : grid.getNodes()) {
            Map<String, Integer> jobs = null;
            String activeId = null;
            if (node instanceof AssemblerBlockEntity assembler) {
                jobs = assembler.getJobSummary();
                for (String id : jobs.keySet()) {
                    if (assembler.isWorkingOn(id)) {
                        activeId = id;
                    }
                }
            } else if (node instanceof PatternProviderBlockEntity provider) {
                jobs = provider.getJobSummary();
                if (!jobs.isEmpty()) {
                    activeId = jobs.keySet().iterator().next();
                }
            }
            if (jobs == null) {
                continue;
            }
            for (Map.Entry<String, Integer> job : jobs.entrySet()) {
                PatternData pattern = findPattern(grid, job.getKey());
                if (pattern == null) {
                    continue;
                }
                QueueEntry entry = result.computeIfAbsent(job.getKey(), k -> new QueueEntry(pattern));
                if (node instanceof AssemblerBlockEntity a) {
                    entry.etaTicks = Math.max(entry.etaTicks, job.getValue() * a.getCraftTicks());
                }
                entry.crafts += job.getValue();
                entry.machines++;
                entry.active |= job.getKey().equals(activeId);
            }
        }
        com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity c = grid.getController();
        synchronized (CraftingService.class) {
            Map<String, Integer> ordered = ORDERED.computeIfAbsent(grid, g -> new HashMap<>());
            ordered.keySet().removeIf(id -> !result.containsKey(id));
            for (QueueEntry e : result.values()) {
                e.ordered = Math.max(e.crafts, ordered.getOrDefault(e.pattern.getId(), e.crafts));
                ordered.put(e.pattern.getId(), e.ordered);
                if (c != null) {
                    e.priority = c.getQueuePriority(e.pattern.getId());
                    e.paused = c.isQueuePaused(e.pattern.getId());
                }
            }
        }
        List<QueueEntry> list = new ArrayList<>(result.values());
        list.sort((a, b) -> Integer.compare(b.priority, a.priority));
        return list;
    }

    /** Скасувати всі замовлення шаблону в усіх машинах мережі. */
    public static void cancel(IGrid grid, String patternId) {
        if (grid.getStorage() != null) {
            grid.getStorage().releaseReservation(patternId);
        }
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof AssemblerBlockEntity assembler) {
                assembler.cancelPattern(patternId);
            } else if (node instanceof PatternProviderBlockEntity provider) {
                provider.cancelPattern(patternId);
            }
        }
    }

    // --- Замовлення з терміналів ---

    /** Шаблон (явний або з Бібліотеки рецептів), що виробляє даний предмет; явні мають пріоритет. */
    @Nullable
    public static PatternData findPatternForOutput(IGrid grid, ItemStack output) {
        String wanted = key(output);
        for (PatternData pattern : allPatternsWithLibrary(grid)) {
            if (key(pattern.getOutput()).equals(wanted)) {
                return pattern;
            }
        }
        return null;
    }


    /** Усі предмети, які мережа вміє крафтити (унікальні виходи шаблонів). */
    public static List<ItemStack> craftableOutputs(IGrid grid) {
        java.util.Map<String, ItemStack> map = new java.util.LinkedHashMap<>();
        for (PatternData pattern : allPatternsWithLibrary(grid)) {
            ItemStack out = pattern.getOutput().copy();
            out.setCount(1);
            map.putIfAbsent(key(out), out);
            if (map.size() >= 2048) {
                break;
            }
        }
        return new java.util.ArrayList<>(map.values());
    }

    /**
     * Замовити автокрафт предмета (кількість у штуках) із повідомленням для гравця:
     * підкрафти включаються, відсутні ресурси та відсутність машин повідомляються.
     */
    public static net.minecraft.network.chat.Component orderOutput(IGrid grid, ItemStack output, int amount) {
        if (grid == null || !grid.getState().isActive()) {
            return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.no_network");
        }
        PatternData found = null;
        String wanted = key(output);
        for (PatternData pattern : allPatternsWithLibrary(grid)) {
            if (key(pattern.getOutput()).equals(wanted)) {
                found = pattern;
                break;
            }
        }
        if (found == null) {
            return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.no_pattern");
        }
        int perCraft = Math.max(1, found.getOutput().getCount());
        int crafts = Math.max(1, Math.min(MAX_CRAFTS, (amount + perCraft - 1) / perCraft));
        Plan plan = plan(grid, found, crafts);
        if (!plan.missing.isEmpty()) {
            ItemStack first = plan.missing.get(0);
            return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.missing",
                    first.getCount(), first.getHoverName());
        }
        for (Step step : plan.steps) {
            String error = routeError(grid, step.pattern);
            if (error != null) {
                return net.minecraft.network.chat.Component.translatable(error);
            }
        }
        int accepted = 0;
        for (Step step : plan.steps) {
            int got = enqueue(grid, step.pattern, step.crafts);
            if (step.pattern.getId().equals(found.getId())) {
                accepted = got;
            }
        }
        if (accepted <= 0) {
            return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.queue_full");
        }
        reservePlan(grid, plan, found.getId());
        return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.ok",
                accepted, found.getOutput().getHoverName());
    }
}
