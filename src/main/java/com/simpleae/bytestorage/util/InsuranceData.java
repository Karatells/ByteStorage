package com.simpleae.bytestorage.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Записи карты «Страховка»: что именно спрятано в сети после смерти игрока и в каком контроллере. */
public class InsuranceData extends SavedData {

    private static final String NAME = "simple_bytestorage_insurance";

    public static final class Record {
        public final String dimension;
        public final BlockPos controller;
        public final List<ItemStack> items = new ArrayList<>();

        public Record(String dimension, BlockPos controller) {
            this.dimension = dimension;
            this.controller = controller;
        }
    }

    private final Map<UUID, Record> records = new HashMap<>();

    public static InsuranceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(InsuranceData::load, InsuranceData::new, NAME);
    }

    public Record get(UUID player) {
        return records.get(player);
    }

    /** Получить запись игрока для данного контроллера (создаётся при необходимости; чужой контроллер перезаписывает). */
    public Record open(UUID player, String dimension, BlockPos controller) {
        Record existing = records.get(player);
        if (existing != null && existing.dimension.equals(dimension) && existing.controller.equals(controller)) {
            return existing;
        }
        Record created = new Record(dimension, controller);
        records.put(player, created);
        setDirty();
        return created;
    }

    public void remove(UUID player) {
        if (records.remove(player) != null) {
            setDirty();
        }
    }

    public void touch() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Record> e : records.entrySet()) {
            CompoundTag r = new CompoundTag();
            r.putUUID("Player", e.getKey());
            r.putString("Dim", e.getValue().dimension);
            r.putLong("Pos", e.getValue().controller.asLong());
            ListTag items = new ListTag();
            for (ItemStack stack : e.getValue().items) {
                CompoundTag t = new CompoundTag();
                ItemStack one = stack.copy();
                one.setCount(1);
                one.save(t);
                t.putInt("N", stack.getCount());
                items.add(t);
            }
            r.put("Items", items);
            list.add(r);
        }
        tag.put("Records", list);
        return tag;
    }

    public static InsuranceData load(CompoundTag tag) {
        InsuranceData data = new InsuranceData();
        for (Tag raw : tag.getList("Records", Tag.TAG_COMPOUND)) {
            CompoundTag r = (CompoundTag) raw;
            Record record = new Record(r.getString("Dim"), BlockPos.of(r.getLong("Pos")));
            for (Tag rawItem : r.getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) rawItem;
                ItemStack stack = ItemStack.of(t);
                stack.setCount(Math.max(1, t.getInt("N")));
                if (!stack.isEmpty()) {
                    record.items.add(stack);
                }
            }
            data.records.put(r.getUUID("Player"), record);
        }
        return data;
    }

}
