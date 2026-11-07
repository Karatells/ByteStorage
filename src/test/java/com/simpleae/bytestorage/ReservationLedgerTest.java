package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.ReservationLedger;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReservationLedgerTest {

    @Test
    void reservesAndSums() {
        ReservationLedger l = new ReservationLedger();
        l.reserve("a", Map.of("iron", 10, "stick", 2));
        l.reserve("b", Map.of("iron", 5));
        assertEquals(15, l.reserved("iron"));
        assertEquals(2, l.reserved("stick"));
        assertEquals(17, l.totalReserved());
        assertFalse(l.isEmpty());
    }

    @Test
    void sameOrderMerges() {
        ReservationLedger l = new ReservationLedger();
        l.reserve("a", Map.of("iron", 4));
        l.reserve("a", Map.of("iron", 6));
        assertEquals(10, l.reserved("iron"));
        l.release("a");
        assertEquals(0, l.reserved("iron"));
        assertTrue(l.isEmpty());
    }

    @Test
    void zeroAmountsAreIgnored() {
        ReservationLedger l = new ReservationLedger();
        l.reserve("a", Map.of("iron", 0));
        assertTrue(l.isEmpty());
    }

    @Test
    void consumeReducesOldestOrderFirst() {
        ReservationLedger l = new ReservationLedger();
        l.reserve("old", Map.of("iron", 3));
        l.reserve("new", Map.of("iron", 4));
        l.consume("iron", 4); // 3 from old, 1 from new
        assertEquals(3, l.reserved("iron"));
        l.release("old"); // already empty
        assertEquals(3, l.reserved("iron"));
        l.release("new");
        assertEquals(0, l.reserved("iron"));
    }

    @Test
    void consumeMoreThanReservedIsSafe() {
        ReservationLedger l = new ReservationLedger();
        l.reserve("a", Map.of("iron", 2));
        l.consume("iron", 100);
        assertTrue(l.isEmpty());
        l.consume("unknown", 5);
        assertTrue(l.isEmpty());
    }

    @Test
    void releaseKeepsOtherOrders() {
        ReservationLedger l = new ReservationLedger();
        l.reserve("a", Map.of("iron", 3, "gold", 1));
        l.reserve("b", Map.of("iron", 2));
        l.release("a");
        assertEquals(2, l.reserved("iron"));
        assertEquals(0, l.reserved("gold"));
    }

    @Test
    void clearEmptiesEverything() {
        ReservationLedger l = new ReservationLedger();
        l.reserve("a", Map.of("iron", 3));
        l.clear();
        assertTrue(l.isEmpty());
        assertEquals(0, l.totalReserved());
    }

    @Test
    void snapshotRoundTrip() {
        ReservationLedger l = new ReservationLedger();
        l.reserve("a", Map.of("iron", 10));
        l.reserve("b", Map.of("iron", 5, "stick", 2));
        ReservationLedger copy = new ReservationLedger();
        copy.restore(l.snapshot());
        assertEquals(15, copy.reserved("iron"));
        assertEquals(2, copy.reserved("stick"));
        copy.release("a");
        assertEquals(5, copy.reserved("iron"));
        assertEquals(15, l.reserved("iron"), "snapshot must be a copy");
    }
}
