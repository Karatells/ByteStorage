package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.StockSchedule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StockScheduleTest {

    @Test
    void alwaysIsAlwaysActive() {
        assertTrue(StockSchedule.isActive(StockSchedule.ALWAYS, 6000, 0, false));
        assertTrue(StockSchedule.isActive(StockSchedule.ALWAYS, 18000, 0, true));
    }

    @Test
    void nightAndDayAreComplements() {
        for (long t = 0; t < 48000; t += 500) {
            boolean night = StockSchedule.isActive(StockSchedule.NIGHT, t, 100, true);
            boolean day = StockSchedule.isActive(StockSchedule.DAY, t, 100, true);
            assertNotEquals(night, day, "t=" + t);
        }
        assertFalse(StockSchedule.isActive(StockSchedule.NIGHT, 6000, 100, true));
        assertTrue(StockSchedule.isActive(StockSchedule.NIGHT, 18000, 100, true));
        assertTrue(StockSchedule.isActive(StockSchedule.NIGHT, 18000 + 24000L * 5, 100, true), "later days");
    }

    @Test
    void energyHasHysteresis() {
        assertFalse(StockSchedule.isActive(StockSchedule.ENERGY, 0, 40, false), "below resume while paused stays paused");
        assertTrue(StockSchedule.isActive(StockSchedule.ENERGY, 0, 50, false));
        assertTrue(StockSchedule.isActive(StockSchedule.ENERGY, 0, 40, true), "above pause while running stays running");
        assertFalse(StockSchedule.isActive(StockSchedule.ENERGY, 0, 24, true));
    }

    @Test
    void namesRoundTrip() {
        for (int i = 0; i < StockSchedule.MODES; i++) {
            assertEquals(i, StockSchedule.byName(StockSchedule.name(i)));
        }
        assertEquals(-1, StockSchedule.byName("nope"));
    }
}
