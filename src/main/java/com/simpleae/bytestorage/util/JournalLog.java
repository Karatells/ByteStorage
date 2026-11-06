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
    public static final int STUCK = 6;        // машина ждёт ресурсы/место слишком долго
    public static final int SCRIPT = 7;       // запись из скрипта (с текстом)
    public static final int SCRIPT_WARN = 8;  // предупреждение из скрипта
    public static final int TYPES = 9;
    public static final int MAX_TEXT = 60;

    public static final int MAX_ENTRIES = 60;
    public static final long MERGE_TICKS = 600;

    public record Entry(long tick, int type, String item, int amount, String text) {
    }

    private final List<Entry> entries = new ArrayList<>(); // от старых к новым

    public void add(long now, int type, String item, int amount) {
        add(now, type, item, amount, "");
    }

    public void add(long now, int type, String item, int amount, String text) {
        if (amount <= 0 || item == null) {
            return;
        }
        if (text == null) {
            text = "";
        } else if (text.length() > MAX_TEXT) {
            text = text.substring(0, MAX_TEXT);
        }
        if (!entries.isEmpty()) {
            Entry last = entries.get(entries.size() - 1);
            if (last.type() == type && last.item().equals(item) && last.text().equals(text) && now - last.tick() <= MERGE_TICKS && now >= last.tick()) {
                int total = type == STOCK_FAILED ? amount : last.amount() + amount;
                entries.set(entries.size() - 1, new Entry(now, type, item, total, text));
                return;
            }
        }
        entries.add(new Entry(now, type, item, amount, text));
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
