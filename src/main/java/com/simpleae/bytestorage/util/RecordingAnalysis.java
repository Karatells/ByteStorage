package com.simpleae.bytestorage.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Разбор наблюдения за машиной при записи шаблона: что израсходовано и что получено.
 * Самый большой прирост — основной результат, остальные — побочные продукты.
 */
public final class RecordingAnalysis {

    /** @param consumed ключ предмета → израсходовано; @param gains приросты по убыванию (ключ, количество) */
    public record Result(Map<String, Integer> consumed, List<Map.Entry<String, Integer>> gains) {
        public boolean valid(int maxInputs) {
            return !consumed.isEmpty() && !gains.isEmpty() && consumed.size() <= maxInputs;
        }
    }

    private RecordingAnalysis() {
    }

    public static Result analyze(Map<String, Integer> start, Map<String, Integer> cur) {
        Map<String, Integer> consumed = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : start.entrySet()) {
            int now = cur.getOrDefault(e.getKey(), 0);
            if (now < e.getValue()) {
                consumed.put(e.getKey(), e.getValue() - now);
            }
        }
        List<Map.Entry<String, Integer>> gains = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cur.entrySet()) {
            int gain = e.getValue() - start.getOrDefault(e.getKey(), 0);
            if (gain > 0 && !consumed.containsKey(e.getKey())) {
                gains.add(Map.entry(e.getKey(), gain));
            }
        }
        gains.sort((a, b) -> a.getValue().equals(b.getValue())
                ? a.getKey().compareTo(b.getKey()) : Integer.compare(b.getValue(), a.getValue()));
        return new Result(consumed, gains);
    }
}
