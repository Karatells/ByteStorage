package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.UserPrefs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPrefsTest {

    @Test
    void favoritesToggleAndLimit() {
        UserPrefs p = new UserPrefs();
        assertTrue(p.setFavorite("a", true));
        assertTrue(p.setFavorite("a", true));
        assertEquals(1, p.favorites().size());
        assertFalse(p.setFavorite("a", false));
        assertTrue(p.favorites().isEmpty());
        for (int i = 0; i < UserPrefs.MAX_FAVORITES; i++) {
            assertTrue(p.setFavorite("k" + i, true));
        }
        assertFalse(p.setFavorite("extra", true));
        assertEquals(UserPrefs.MAX_FAVORITES, p.favorites().size());
    }

    @Test
    void invalidKeysAreIgnored() {
        UserPrefs p = new UserPrefs();
        assertFalse(p.setFavorite("", true));
        assertFalse(p.setFavorite(null, true));
        assertFalse(p.setFavorite("x".repeat(UserPrefs.MAX_KEY_LENGTH + 1), true));
        assertTrue(p.isEmpty());
    }

    @Test
    void searchesAreCleaned() {
        UserPrefs p = new UserPrefs();
        p.setSearch(0, "  iron\nore ");
        p.setSearch(1, "x".repeat(200));
        p.setSearch(7, "ignored");
        assertEquals("iron ore", p.searches().get(0));
        assertEquals(UserPrefs.MAX_SEARCH_LENGTH, p.searches().get(1).length());
        assertEquals("", p.searches().get(2));
        assertFalse(p.isEmpty());
    }

    @Test
    void mergeKeepsExistingSearches() {
        UserPrefs p = new UserPrefs();
        p.setFavorite("a", true);
        p.setSearch(0, "mine");
        p.merge(List.of("a", "b"), List.of("old0", "old1", "old2"));
        assertEquals(List.of("a", "b"), p.favoriteList());
        assertEquals(List.of("mine", "old1", "old2"), p.searches());
    }
}
