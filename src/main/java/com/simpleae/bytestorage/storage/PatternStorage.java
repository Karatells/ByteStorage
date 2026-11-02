package com.simpleae.bytestorage.storage;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Реалізація глобального сховища шаблонів мережі.
 *
 * Живе всередині {@link com.simpleae.bytestorage.network.GridNetwork}. Збереження на диск виконує
 * контролер мережі (див. NetworkControllerBlockEntity), а про кожну зміну сповіщається слухач,
 * який позначає контролер «брудним».
 */
public class PatternStorage implements IPatternStorage {

    private final Map<String, PatternData> patterns = new LinkedHashMap<>();
    private int version = 0;
    private Runnable changeListener = () -> {};

    public void setChangeListener(Runnable listener) {
        this.changeListener = listener == null ? () -> {} : listener;
    }

    private void changed() {
        version++;
        changeListener.run();
    }

    @Override
    public PatternData addPattern(ItemStack output, List<ItemStack> grid) {
        PatternData data = new PatternData(output, grid);
        PatternData existing = patterns.get(data.getId());
        if (existing != null) {
            return existing;
        }
        patterns.put(data.getId(), data);
        changed();
        return data;
    }

    @Override
    public boolean removePattern(String id) {
        if (patterns.remove(id) != null) {
            changed();
            return true;
        }
        return false;
    }

    @Override
    public PatternData getPattern(String id) {
        return id == null ? null : patterns.get(id);
    }

    @Override
    public Collection<PatternData> getAllPatterns() {
        return Collections.unmodifiableList(new ArrayList<>(patterns.values()));
    }

    @Override
    public boolean canCraft(IGrid grid, String id) {
        PatternData pattern = getPattern(id);
        if (pattern == null || grid == null) {
            return false;
        }
        INetworkStorage storage = grid.getStorage();
        if (storage == null) {
            return false;
        }
        for (ItemStack input : pattern.getInputs()) {
            ItemStack available = storage.extractItem(input, input.getCount(), true);
            if (available.isEmpty() || available.getCount() < input.getCount()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        return patterns.size();
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public ListTag save() {
        ListTag list = new ListTag();
        for (PatternData data : patterns.values()) {
            list.add(data.save());
        }
        return list;
    }

    @Override
    public void load(ListTag tag) {
        patterns.clear();
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entry = tag.getCompound(i);
            PatternData data = PatternData.load(entry);
            if (!data.getOutput().isEmpty()) {
                patterns.put(data.getId(), data);
            }
        }
        version++;
    }

    public void clear() {
        if (!patterns.isEmpty()) {
            patterns.clear();
            changed();
        }
    }
}
