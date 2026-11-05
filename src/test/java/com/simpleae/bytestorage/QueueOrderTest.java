package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.QueueOrder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueueOrderTest {

    private static int pick(List<String> ids, Set<String> paused, Map<String, Integer> prio) {
        return QueueOrder.pick(ids, paused::contains, id -> prio.getOrDefault(id, 0));
    }

    @Test
    void emptyQueueGivesMinusOne() {
        assertEquals(-1, pick(List.of(), Set.of(), Map.of()));
    }

    @Test
    void equalPriorityKeepsQueueOrder() {
        assertEquals(0, pick(List.of("a", "b", "c"), Set.of(), Map.of()));
    }

    @Test
    void highestPriorityWins() {
        assertEquals(2, pick(List.of("a", "b", "c"), Set.of(), Map.of("c", 3, "b", 1)));
    }

    @Test
    void negativePriorityGoesLast() {
        assertEquals(1, pick(List.of("a", "b"), Set.of(), Map.of("a", -2)));
    }

    @Test
    void pausedAreSkippedEvenWithHighPriority() {
        assertEquals(0, pick(List.of("a", "b"), Set.of("b"), Map.of("b", 9)));
    }

    @Test
    void allPausedGivesMinusOne() {
        assertEquals(-1, pick(List.of("a", "b"), Set.of("a", "b"), Map.of()));
    }

    @Test
    void tieBetweenEqualPrioritiesPicksEarlier() {
        assertEquals(1, pick(List.of("a", "b", "c"), Set.of("a"), Map.of("b", 2, "c", 2)));
    }

    @Test
    void etaFormatting() {
        assertEquals("0:00", QueueOrder.formatEta(0));
        assertEquals("0:09", QueueOrder.formatEta(9));
        assertEquals("1:15", QueueOrder.formatEta(75));
        assertEquals("59:59", QueueOrder.formatEta(3599));
        assertEquals("1h02", QueueOrder.formatEta(3725));
    }
}
