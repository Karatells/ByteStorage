package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.CapacitorLogic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CapacitorLogicTest {

    @Test
    void refillsLowController() {
        // 20% full, capacitor has plenty: tops the controller up towards 60%, limited per transfer
        assertEquals(10_000, CapacitorLogic.transfer(20_000, 100_000, 400_000, 500_000, 10_000));
        assertEquals(40_000, CapacitorLogic.transfer(20_000, 100_000, 400_000, 500_000, 100_000));
    }

    @Test
    void refillIsLimitedByCapacitorContent() {
        assertEquals(300, CapacitorLogic.transfer(20_000, 100_000, 300, 500_000, 10_000));
        assertEquals(0, CapacitorLogic.transfer(20_000, 100_000, 0, 500_000, 10_000));
    }

    @Test
    void absorbsSurplusWhenControllerIsFull() {
        assertEquals(-10_000, CapacitorLogic.transfer(100_000, 100_000, 0, 500_000, 10_000));
        assertEquals(-15_000, CapacitorLogic.transfer(100_000, 100_000, 0, 500_000, 50_000));
        assertEquals(-100, CapacitorLogic.transfer(100_000, 100_000, 499_900, 500_000, 10_000));
        assertEquals(0, CapacitorLogic.transfer(100_000, 100_000, 500_000, 500_000, 10_000));
    }

    @Test
    void middleRangeIsLeftAlone() {
        for (int stored = 50_000; stored <= 90_000; stored += 5_000) {
            assertEquals(0, CapacitorLogic.transfer(stored, 100_000, 250_000, 500_000, 10_000), "stored=" + stored);
        }
    }

    @Test
    void zeroCapacityControllerIsSafe() {
        assertEquals(0, CapacitorLogic.transfer(0, 0, 100, 500, 10));
    }
}
