package com.simpleae.bytestorage.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * История сети для графиков Контроллера: периодические снимки запасов и заряда (чистая логика, покрыта тестами).
 */
public final class StatsHistory {

    public static final int MAX_SAMPLES = 60;
    /** Как часто контроллер делает снимок. */
    public static final int SAMPLE_TICKS = 400;
    public static final int SAMPLE_SECONDS = SAMPLE_TICKS / 20;

    public record Change(String key, int delta) {
    }

    private record Sample(Map<String, Integer> counts, long total, int energyPercent) {
    }

    private final ArrayDeque<Sample> samples = new ArrayDeque<>();

    public void record(Map<String, Integer> counts, int energyPercent) {
        long total = 0;
        for (int v : counts.values()) {
            total += v;
        }
        samples.addLast(new Sample(new HashMap<>(counts), total, Math.max(0, Math.min(100, energyPercent))));
        while (samples.size() > MAX_SAMPLES) {
            samples.removeFirst();
        }
    }

    public int size() {
        return samples.size();
    }

    /** Сколько секунд охватывает история (от самого старого снимка до самого нового). */
    public int spanSeconds() {
        return Math.max(0, samples.size() - 1) * SAMPLE_SECONDS;
    }

    /** Суммарное количество предметов по снимкам, от старых к новым. */
    public int[] totalSeries() {
        int[] result = new int[samples.size()];
        int i = 0;
        for (Sample s : samples) {
            result[i++] = (int) Math.min(Integer.MAX_VALUE, s.total());
        }
        return result;
    }

    public int[] energySeries() {
        int[] result = new int[samples.size()];
        int i = 0;
        for (Sample s : samples) {
            result[i++] = s.energyPercent();
        }
        return result;
    }

    /**
     * До {@code n} предметов с наибольшим ростом ({@code gain} = true) или убылью за весь период истории.
     * Нулевые изменения не возвращаются; убыль отдаётся отрицательными числами.
     */
    public List<Change> topChanges(int n, boolean gain) {
        List<Change> result = new ArrayList<>();
        if (samples.size() < 2) {
            return result;
        }
        Map<String, Integer> first = samples.peekFirst().counts();
        Map<String, Integer> last = samples.peekLast().counts();
        Set<String> keys = new HashSet<>(first.keySet());
        keys.addAll(last.keySet());
        for (String key : keys) {
            int delta = last.getOrDefault(key, 0) - first.getOrDefault(key, 0);
            if (gain ? delta > 0 : delta < 0) {
                result.add(new Change(key, delta));
            }
        }
        Comparator<Change> byDelta = Comparator.comparingInt(Change::delta);
        result.sort(gain ? byDelta.reversed().thenComparing(Change::key) : byDelta.thenComparing(Change::key));
        return result.size() > n ? new ArrayList<>(result.subList(0, n)) : result;
    }
}
