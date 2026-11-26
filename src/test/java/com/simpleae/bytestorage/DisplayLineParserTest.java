package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.DisplayLineParser;
import com.simpleae.bytestorage.util.DisplayLineParser.ParsedLine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisplayLineParserTest {

    @Test
    void plainAndEmptyLines() {
        ParsedLine empty = DisplayLineParser.parse("");
        assertEquals("", empty.text());
        assertFalse(empty.isBar());
        assertEquals(DisplayLineParser.DEFAULT_COLOR, empty.color());

        ParsedLine plain = DisplayLineParser.parse("Iron: 128");
        assertEquals("Iron: 128", plain.text());
        assertFalse(plain.isBar());
    }

    @Test
    void colorPrefixParsedAndStripped() {
        ParsedLine green = DisplayLineParser.parse("&aOnline");
        assertEquals("Online", green.text());
        assertEquals(0xFF4ADE80, green.color());

        ParsedLine red = DisplayLineParser.parse("&cLow Energy!");
        assertEquals("Low Energy!", red.text());
        assertEquals(0xFFF87171, red.color());
    }

    @Test
    void progressBarParsedAndClamped() {
        ParsedLine bar = DisplayLineParser.parse("[bar:75:green]");
        assertTrue(bar.isBar());
        assertEquals(75, bar.barPercent());
        assertEquals(0xFF4ADE80, bar.color());
        assertEquals("75%", bar.text());

        ParsedLine over = DisplayLineParser.parse("[bar:150]");
        assertTrue(over.isBar());
        assertEquals(100, over.barPercent());

        ParsedLine under = DisplayLineParser.parse("[bar:-20]");
        assertTrue(under.isBar());
        assertEquals(0, under.barPercent());
    }

    @Test
    void invalidBarFallsBackToText() {
        ParsedLine bad = DisplayLineParser.parse("[bar:abc]");
        assertFalse(bad.isBar());
        assertEquals("[bar:abc]", bad.text());
    }
}
