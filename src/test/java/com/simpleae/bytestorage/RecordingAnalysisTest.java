package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.RecordingAnalysis;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordingAnalysisTest {

    @Test
    void singleOutput() {
        RecordingAnalysis.Result r = RecordingAnalysis.analyze(Map.of("ore", 1), Map.of("ingot", 1));
        assertEquals(Map.of("ore", 1), r.consumed());
        assertEquals(1, r.gains().size());
        assertEquals("ingot", r.gains().get(0).getKey());
        assertTrue(r.valid(9));
    }

    @Test
    void byproductsAreSortedAfterMainOutput() {
        RecordingAnalysis.Result r = RecordingAnalysis.analyze(Map.of("ore", 2),
                Map.of("dust", 4, "stone", 1, "nugget", 1));
        assertEquals("dust", r.gains().get(0).getKey());
        assertEquals("nugget", r.gains().get(1).getKey());
        assertEquals("stone", r.gains().get(2).getKey());
    }

    @Test
    void consumedItemIsNeverAnOutput() {
        RecordingAnalysis.Result r = RecordingAnalysis.analyze(Map.of("a", 5, "b", 5), Map.of("a", 3, "b", 7));
        assertEquals(Map.of("a", 2), r.consumed());
        assertEquals(1, r.gains().size());
        assertEquals("b", r.gains().get(0).getKey());
    }

    @Test
    void invalidWhenNothingHappened() {
        assertFalse(RecordingAnalysis.analyze(Map.of("a", 1), Map.of("a", 1)).valid(9));
        assertFalse(RecordingAnalysis.analyze(Map.of(), Map.of("x", 1)).valid(9));
    }
}
