package com.simpleae.bytestorage.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Журнал сети: последние события автокрафта (чистая логика без классов Minecraft — покрыта тестами).
 * Подряд идущие однотипные события по одному предмету за короткое время сливаются в одну запись.
 */
public final class JournalLog {

    public static final int ORDER = 0;        // заказ принят в очередь
    public static final int CRAFTED = 1;      // Асемблер закончил крафт
    public static final int STARTED = 2;      // Шина подала крафт в машину
    public static final int STOCK_ORDER = 3;  // правило запаса сделало заказ
    public static final int STOCK_FAILED = 4; // правило запаса не смогло заказать
    public static final int CANCELLED = 5;    // заказ отменён
    public static final int TYPES = 6;

    public static final int MAX_ENTRIES = 60;
    public static final long MERGE_TICKS = 600;

    public record Entry(long tick, int type, String item, int amount) {
    }

    private final List<Entry> entries = new ArrayList<>(); // от старых к новым

    public void add(long now, int type, String item, int amount) {
        if (amount <= 0 || item == null) {
            return;
        }
        if (!entries.isEmpty()) {
            Entry last = entries.get(entries.size() - 1);
            if (last.type() == type && last.item().equals(item) && now - last.tick() <= MERGE_TICKS && now >= last.tick()) {
                int total = type == STOCK_FAILED ? amount : last.amount() + amount;
                entries.set(entries.size() - 1, new Entry(now, type, item, total));
                return;
            }
        }
        entries.add(new Entry(now, type, item, amount));
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
    }

    /** До {@code n} последних записей, самые новые первыми. */
    public List<Entry> latest(int n) {
        List<Entry> result = new ArrayList<>(Math.min(n, entries.size()));
        for (int i = entries.size() - 1; i >= 0 && result.size() < n; i--) {
            result.add(entries.get(i));
        }
        return result;
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }
}
