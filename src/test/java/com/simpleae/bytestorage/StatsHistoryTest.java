package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.StatsHistory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatsHistoryTest {

    @Test
    void seriesKeepOrderAndSums() {
        StatsHistory h = new StatsHistory();
        h.record(Map.of("a", 10, "b", 5), 80);
        h.record(Map.of("a", 12, "b", 5), 60);
        assertArrayEquals(new int[]{15, 17}, h.totalSeries());
        assertArrayEquals(new int[]{80, 60}, h.energySeries());
        assertEquals(StatsHistory.SAMPLE_SECONDS, h.spanSeconds());
    }

    @Test
    void historyIsBounded() {
        StatsHistory h = new StatsHistory();
        for (int i = 0; i < StatsHistory.MAX_SAMPLES + 15; i++) {
            h.record(Map.of("a", i), 50);
        }
        assertEquals(StatsHistory.MAX_SAMPLES, h.size());
        assertEquals(15, h.totalSeries()[0], "oldest samples are dropped");
    }

    @Test
    void topChangesGainersAndLosers() {
        StatsHistory h = new StatsHistory();
        h.record(Map.of("iron", 100, "coal", 50, "gold", 10), 100);
        h.record(Map.of("iron", 40, "coal", 90, "gold", 10, "new", 7), 100);
        List<StatsHistory.Change> gain = h.topChanges(3, true);
        assertEquals("coal", gain.get(0).key());
        assertEquals(40, gain.get(0).delta());
        assertEquals("new", gain.get(1).key());
        List<StatsHistory.Change> lose = h.topChanges(3, false);
        assertEquals(1, lose.size());
        assertEquals(-60, lose.get(0).delta());
    }

    @Test
    void needsTwoSamplesForChanges() {
        StatsHistory h = new StatsHistory();
        h.record(Map.of("a", 1), 10);
        assertTrue(h.topChanges(3, true).isEmpty());
        assertEquals(0, h.spanSeconds());
    }
}
