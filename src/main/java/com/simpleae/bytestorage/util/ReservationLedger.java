package com.simpleae.bytestorage.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Книга резервов сети: какое сырьё забронировано под принятые заказы (чистая логика, покрыта тестами).
 * Резерв суммируется по ключу предмета; по заказам хранится остаток, чтобы отмену можно было снять целиком.
 */
public final class ReservationLedger {

    private final Map<String, Integer> total = new HashMap<>();
    private final LinkedHashMap<String, Map<String, Integer>> byOrder = new LinkedHashMap<>();

    public void reserve(String orderId, Map<String, Integer> amounts) {
        Map<String, Integer> order = byOrder.computeIfAbsent(orderId, k -> new HashMap<>());
        for (Map.Entry<String, Integer> e : amounts.entrySet()) {
            if (e.getValue() > 0) {
                order.merge(e.getKey(), e.getValue(), Integer::sum);
                total.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        if (order.isEmpty()) {
            byOrder.remove(orderId);
        }
    }

    /** Снять остаток резерва заказа (отмена). */
    public void release(String orderId) {
        Map<String, Integer> order = byOrder.remove(orderId);
        if (order != null) {
            order.forEach(this::subtract);
        }
    }

    /** Машина забрала {@code amount} предметов для крафта: резерв уменьшается, начиная с самых старых заказов. */
    public void consume(String key, int amount) {
        int remaining = amount;
        Iterator<Map.Entry<String, Map<String, Integer>>> it = byOrder.entrySet().iterator();
        while (it.hasNext() && remaining > 0) {
            Map<String, Integer> order = it.next().getValue();
            Integer have = order.get(key);
            if (have == null) {
                continue;
            }
            int take = Math.min(have, remaining);
            remaining -= take;
            subtract(key, take);
            if (have - take <= 0) {
                order.remove(key);
            } else {
                order.put(key, have - take);
            }
            if (order.isEmpty()) {
                it.remove();
            }
        }
    }

    public int reserved(String key) {
        return total.getOrDefault(key, 0);
    }

    public int totalReserved() {
        int sum = 0;
        for (int v : total.values()) {
            sum += v;
        }
        return sum;
    }

    public boolean isEmpty() {
        return total.isEmpty();
    }

    public void clear() {
        total.clear();
        byOrder.clear();
    }

    private void subtract(String key, int amount) {
        int left = total.getOrDefault(key, 0) - amount;
        if (left > 0) {
            total.put(key, left);
        } else {
            total.remove(key);
        }
    }
}
