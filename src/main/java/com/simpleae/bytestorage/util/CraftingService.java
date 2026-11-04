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
        return null;
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
        Map<String, PatternData> producers = new HashMap<>();
        for (PatternData pattern : allPatterns(grid)) {
            producers.putIfAbsent(key(pattern.getOutput()), pattern);
        }

        Map<String, Step> steps = new LinkedHashMap<>();
        Deque<String> path = new ArrayDeque<>();
        int crafts = Math.max(1, Math.min(MAX_CRAFTS, amount));
        expand(root, crafts, available, producers, steps, plan.missing, path);
        plan.steps.addAll(steps.values());
        return plan;
    }

    private static void expand(PatternData pattern, int crafts, Map<String, Integer> available,
                               Map<String, PatternData> producers, Map<String, Step> steps,
                               List<ItemStack> missing, Deque<String> path) {
        path.push(pattern.getId());
        for (ItemStack input : pattern.getInputs()) {
            require(input, (long) input.getCount() * crafts, available, producers, steps, missing, path);
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
                                List<ItemStack> missing, Deque<String> path) {
        String k = key(template);
        int have = available.getOrDefault(k, 0);
        long use = Math.min(have, count);
        available.put(k, (int) (have - use));
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
        expand(producer, crafts, available, producers, steps, missing, path);
        long leftover = (long) crafts * perCraft - remaining;
        if (leftover > 0) {
            available.merge(k, (int) Math.min(Integer.MAX_VALUE, leftover), Integer::sum);
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
        return accepted;
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
        for (PatternData pattern : allPatterns(grid)) {
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
        return accepted;
    }

    // --- Черга мережі ---

    /** Рядок зведеної черги мережі. */
    public static final class QueueEntry {
        public final PatternData pattern;
        public int crafts;
        public boolean active;
        public int machines;

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
                entry.crafts += job.getValue();
                entry.machines++;
                entry.active |= job.getKey().equals(activeId);
            }
        }
        return new ArrayList<>(result.values());
    }

    /** Скасувати всі замовлення шаблону в усіх машинах мережі. */
    public static void cancel(IGrid grid, String patternId) {
        for (IGridNode node : grid.getNodes()) {
            if (node instanceof AssemblerBlockEntity assembler) {
                assembler.cancelPattern(patternId);
            } else if (node instanceof PatternProviderBlockEntity provider) {
                provider.cancelPattern(patternId);
            }
        }
    }

    // --- Замовлення з терміналів ---

    /** Усі предмети, які мережа вміє крафтити (унікальні виходи шаблонів). */
    public static List<ItemStack> craftableOutputs(IGrid grid) {
        java.util.Map<String, ItemStack> map = new java.util.LinkedHashMap<>();
        for (PatternData pattern : allPatterns(grid)) {
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
        for (PatternData pattern : allPatterns(grid)) {
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
        return net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.order.ok",
                accepted, found.getOutput().getHoverName());
    }
}
