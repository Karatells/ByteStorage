package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.JournalLog;
import com.simpleae.bytestorage.util.JournalLog.Entry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalLogTest {

    @Test
    void newestFirstAndLimited() {
        JournalLog log = new JournalLog();
        log.add(0, JournalLog.ORDER, "a", 1);
        log.add(1, JournalLog.CRAFTED, "b", 1);
        log.add(2, JournalLog.CANCELLED, "c", 1);
        List<Entry> latest = log.latest(2);
        assertEquals(2, latest.size());
        assertEquals("c", latest.get(0).item());
        assertEquals("b", latest.get(1).item());
    }

    @Test
    void sameEventsWithinWindowMerge() {
        JournalLog log = new JournalLog();
        log.add(0, JournalLog.CRAFTED, "planks", 4);
        log.add(100, JournalLog.CRAFTED, "planks", 4);
        log.add(200, JournalLog.CRAFTED, "planks", 4);
        assertEquals(1, log.size());
        assertEquals(12, log.latest(1).get(0).amount());
        assertEquals(200, log.latest(1).get(0).tick());
    }

    @Test
    void doesNotMergeAfterWindowOrDifferentTypeOrItem() {
        JournalLog log = new JournalLog();
        log.add(0, JournalLog.CRAFTED, "planks", 1);
        log.add(JournalLog.MERGE_TICKS + 1, JournalLog.CRAFTED, "planks", 1);
        log.add(JournalLog.MERGE_TICKS + 2, JournalLog.ORDER, "planks", 1);
        log.add(JournalLog.MERGE_TICKS + 3, JournalLog.ORDER, "stick", 1);
        assertEquals(4, log.size());
    }

    @Test
    void stockFailedKeepsLatestAmountInsteadOfSumming() {
        JournalLog log = new JournalLog();
        log.add(0, JournalLog.STOCK_FAILED, "iron", 10);
        log.add(60, JournalLog.STOCK_FAILED, "iron", 7);
        assertEquals(1, log.size());
        assertEquals(7, log.latest(1).get(0).amount());
    }

    @Test
    void ignoresEmptyAmountsAndTrimsOldEntries() {
        JournalLog log = new JournalLog();
        log.add(0, JournalLog.ORDER, "a", 0);
        log.add(0, JournalLog.ORDER, null, 1);
        assertEquals(0, log.size());
        for (int i = 0; i < JournalLog.MAX_ENTRIES + 20; i++) {
            log.add(i * 10_000L, JournalLog.ORDER, "item" + i, 1);
        }
        assertEquals(JournalLog.MAX_ENTRIES, log.size());
        assertTrue(log.latest(JournalLog.MAX_ENTRIES + 20).stream().noneMatch(e -> e.item().equals("item0")));
    }
}
