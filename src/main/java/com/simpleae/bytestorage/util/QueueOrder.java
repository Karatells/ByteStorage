package com.simpleae.bytestorage.util;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Выбор следующего заказа очереди и форматирование времени (чистая логика, покрыта тестами). */
public final class QueueOrder {

    private QueueOrder() {
    }

    /**
     * Какой заказ запускать: наибольший приоритет среди не поставленных на паузу; при равенстве — более ранний.
     * @return индекс или -1, если все на паузе (или список пуст)
     */
    public static int pick(List<String> ids, Predicate<String> paused, ToIntFunction<String> priority) {
        int best = -1;
        int bestPriority = Integer.MIN_VALUE;
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            if (paused.test(id)) {
                continue;
            }
            int p = priority.applyAsInt(id);
            if (p > bestPriority) {
                bestPriority = p;
                best = i;
            }
        }
        return best;
    }

    /** 75 -> "1:15", 3725 -> "1h02". */
    public static String formatEta(int seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + "h" + String.format("%02d", seconds / 60 % 60);
        }
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
