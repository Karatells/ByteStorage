package com.simpleae.bytestorage.util;

import com.simpleae.bytestorage.api.network.IGrid;

import java.util.HashMap;
import java.util.Map;

/** Для подсказки терминала: сколько предмета зарезервировано и сколько сейчас в производстве. */
public final class TerminalAvailability {

    public final Map<String, Integer> reserved = new HashMap<>();
    public final Map<String, Integer> crafting = new HashMap<>();

    public static TerminalAvailability compute(IGrid grid) {
        TerminalAvailability result = new TerminalAvailability();
        if (grid == null || grid.getStorage() == null) {
            return result;
        }
        grid.getStorage().getReservationSnapshot()
                .values().forEach(order -> order.forEach((k, v) -> result.reserved.merge(k, v, Integer::sum)));
        for (CraftingService.QueueEntry entry : CraftingService.queue(grid)) {
            int perCraft = Math.max(1, entry.pattern.getOutput().getCount());
            result.crafting.merge(CraftingService.key(entry.pattern.getOutput()), entry.crafts * perCraft, Integer::sum);
        }
        return result;
    }
}
