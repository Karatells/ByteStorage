package com.simpleae.bytestorage.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Личные настройки игрока, хранимые на сервере: избранные предметы и сохранённые поиски терминала.
 * Чистая логика (без Minecraft) — покрыта тестами; в NBT игрока и в пакеты переносится адаптерами.
 */
public final class UserPrefs {

    public static final int MAX_FAVORITES = 500;
    public static final int SEARCH_SLOTS = 3;
    public static final int MAX_SEARCH_LENGTH = 64;
    public static final int MAX_KEY_LENGTH = 300;

    private final Set<String> favorites = new LinkedHashSet<>();
    private final String[] searches = new String[SEARCH_SLOTS];

    public UserPrefs() {
        java.util.Arrays.fill(searches, "");
    }

    public Set<String> favorites() {
        return java.util.Collections.unmodifiableSet(favorites);
    }

    public List<String> searches() {
        return List.of(searches);
    }

    public boolean isEmpty() {
        return favorites.isEmpty() && java.util.Arrays.stream(searches).allMatch(String::isEmpty);
    }

    private static boolean validKey(String key) {
        return key != null && !key.isEmpty() && key.length() <= MAX_KEY_LENGTH;
    }

    /** @return true, если после вызова предмет в избранном */
    public boolean setFavorite(String key, boolean on) {
        if (!validKey(key)) {
            return false;
        }
        if (!on) {
            favorites.remove(key);
            return false;
        }
        if (favorites.contains(key)) {
            return true;
        }
        if (favorites.size() >= MAX_FAVORITES) {
            return false;
        }
        favorites.add(key);
        return true;
    }

    public void setSearch(int slot, String text) {
        if (slot < 0 || slot >= SEARCH_SLOTS) {
            return;
        }
        searches[slot] = clean(text);
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        String t = text.replace('\n', ' ').replace('\r', ' ').trim();
        return t.length() > MAX_SEARCH_LENGTH ? t.substring(0, MAX_SEARCH_LENGTH) : t;
    }

    /** Слияние данных со старого клиента: недостающие избранные добавляются, пустые ячейки поиска заполняются. */
    public void merge(Collection<String> otherFavorites, List<String> otherSearches) {
        for (String key : otherFavorites) {
            setFavorite(key, true);
        }
        for (int i = 0; i < SEARCH_SLOTS && i < otherSearches.size(); i++) {
            if (searches[i].isEmpty()) {
                searches[i] = clean(otherSearches.get(i));
            }
        }
    }

    /** Копия избранного для передачи (в стабильном порядке). */
    public List<String> favoriteList() {
        return new ArrayList<>(favorites);
    }
}
