package com.simpleae.bytestorage.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Хранение {@link UserPrefs} в постоянных данных игрока (переживают смерть и перезапуск сервера). */
public final class UserPrefsStore {

    public static final String KEY = "simple_bytestorage_prefs";

    private UserPrefsStore() {
    }

    public static UserPrefs load(ServerPlayer player) {
        UserPrefs prefs = new UserPrefs();
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(KEY, Tag.TAG_COMPOUND)) {
            return prefs;
        }
        CompoundTag tag = persisted.getCompound(KEY);
        List<String> favorites = new ArrayList<>();
        ListTag favList = tag.getList("Favorites", Tag.TAG_STRING);
        for (int i = 0; i < favList.size(); i++) {
            favorites.add(favList.getString(i));
        }
        List<String> searches = new ArrayList<>();
        ListTag searchList = tag.getList("Searches", Tag.TAG_STRING);
        for (int i = 0; i < searchList.size(); i++) {
            searches.add(searchList.getString(i));
        }
        prefs.merge(favorites, searches);
        return prefs;
    }

    public static void save(ServerPlayer player, UserPrefs prefs) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tag = new CompoundTag();
        ListTag favList = new ListTag();
        for (String key : prefs.favorites()) {
            favList.add(StringTag.valueOf(key));
        }
        tag.put("Favorites", favList);
        ListTag searchList = new ListTag();
        for (String s : prefs.searches()) {
            searchList.add(StringTag.valueOf(s));
        }
        tag.put("Searches", searchList);
        persisted.put(KEY, tag);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Копирует данные при возрождении, если Forge не перенёс их сам. */
    public static void copy(ServerPlayer from, ServerPlayer to) {
        CompoundTag old = from.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (old.contains(KEY, Tag.TAG_COMPOUND)) {
            CompoundTag root = to.getPersistentData();
            CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
            persisted.put(KEY, old.getCompound(KEY).copy());
            root.put(Player.PERSISTED_NBT_TAG, persisted);
        }
    }
}
