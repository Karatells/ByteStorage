package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.SearchSyntax;
import com.simpleae.bytestorage.util.SearchSyntax.Kind;
import com.simpleae.bytestorage.util.SearchSyntax.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchSyntaxTest {

    @Test
    void emptyAndNullGiveNoTokens() {
        assertTrue(SearchSyntax.parse(null).isEmpty());
        assertTrue(SearchSyntax.parse("   ").isEmpty());
    }

    @Test
    void plainWordsAreText() {
        List<Token> t = SearchSyntax.parse("Iron  Ingot");
        assertEquals(2, t.size());
        assertEquals(Kind.TEXT, t.get(0).kind());
        assertEquals("iron", t.get(0).text());
        assertEquals("ingot", t.get(1).text());
    }

    @Test
    void modAndTag() {
        List<Token> t = SearchSyntax.parse("@minecraft #ingots");
        assertEquals(Kind.MOD, t.get(0).kind());
        assertEquals("minecraft", t.get(0).text());
        assertEquals(Kind.TAG, t.get(1).kind());
        assertEquals("ingots", t.get(1).text());
    }

    @Test
    void lonelySignsAreJustText() {
        for (String s : new String[]{"@", "#", ">", "<", "=", "$"}) {
            List<Token> t = SearchSyntax.parse(s);
            assertEquals(1, t.size());
            assertEquals(Kind.TEXT, t.get(0).kind(), s);
        }
    }

    @Test
    void countComparisons() {
        Token gt = SearchSyntax.parse(">100").get(0);
        Token lt = SearchSyntax.parse("<100").get(0);
        Token eq = SearchSyntax.parse("=100").get(0);
        assertEquals(Kind.COUNT, gt.kind());
        assertTrue(SearchSyntax.countMatches(gt, 101));
        assertFalse(SearchSyntax.countMatches(gt, 100));
        assertTrue(SearchSyntax.countMatches(lt, 99));
        assertFalse(SearchSyntax.countMatches(lt, 100));
        assertTrue(SearchSyntax.countMatches(eq, 100));
        assertFalse(SearchSyntax.countMatches(eq, 99));
        // предметы «только крафт» имеют количество 0
        assertTrue(SearchSyntax.countMatches(SearchSyntax.parse("=0").get(0), 0));
        assertTrue(SearchSyntax.countMatches(SearchSyntax.parse("<1").get(0), 0));
    }

    @Test
    void invalidCountIsInvalidToken() {
        assertEquals(Kind.INVALID, SearchSyntax.parse(">abc").get(0).kind());
        assertEquals(Kind.INVALID, SearchSyntax.parse(">99999999999").get(0).kind());
    }

    @Test
    void flagsResolveByPrefix() {
        assertEquals(SearchSyntax.FLAG_CRAFTABLE, SearchSyntax.parse("$craftable").get(0).text());
        assertEquals(SearchSyntax.FLAG_CRAFTABLE, SearchSyntax.parse("$craft").get(0).text());
        assertEquals(SearchSyntax.FLAG_STOCK, SearchSyntax.parse("$s").get(0).text());
        assertEquals(SearchSyntax.FLAG_FAV, SearchSyntax.parse("$fav").get(0).text());
        assertEquals(Kind.INVALID, SearchSyntax.parse("$nonsense").get(0).kind());
        assertNull(SearchSyntax.resolveFlag(""));
    }

    @Test
    void mixedQuery() {
        List<Token> t = SearchSyntax.parse("@minecraft #ingots >64 $stock gold");
        assertEquals(5, t.size());
        assertEquals(Kind.FLAG, t.get(3).kind());
        assertEquals(Kind.TEXT, t.get(4).kind());
    }
}
