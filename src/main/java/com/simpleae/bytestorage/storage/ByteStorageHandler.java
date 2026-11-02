package com.simpleae.bytestorage.storage;

import com.simpleae.bytestorage.api.storage.ICellInventory;
import com.simpleae.bytestorage.api.storage.IDriveHost;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Менеджер байтового віртуального сховища (ByteStorageHandler).
 * 
 * Реалізує логіку «Віртуального пулу предметів»:
 * - Немає прив'язки до окремих слотів дисків.
 * - Агрегує всі диски всіх підключених Drive блоків.
 * - Контролює глобальне правило: 1 предмет = 1 байт.
 * - При вставці оптимізує розміщення (спочатку доповнює вже існуючі пачки в дисках,
 *   а потім заповнює вільний простір, щоб уникнути фрагментації).
 */
public class ByteStorageHandler implements INetworkStorage {

    private final Set<IDriveHost> connectedDrives = new HashSet<>();
    private List<ItemStack> cachedVirtualPool = null;
    private int cachedTotalCapacity = -1;
    private int cachedTotalUsed = -1;

    @Override
    public void attachDrive(IDriveHost drive) {
        if (connectedDrives.add(drive)) {
            invalidateCache();
        }
    }

    @Override
    public void detachDrive(IDriveHost drive) {
        if (connectedDrives.remove(drive)) {
            invalidateCache();
        }
    }

    @Override
    public void invalidateCache() {
        this.cachedVirtualPool = null;
        this.cachedTotalCapacity = -1;
        this.cachedTotalUsed = -1;
    }

    @Override
    public int getTotalCapacityBytes() {
        if (cachedTotalCapacity != -1) {
            return cachedTotalCapacity;
        }

        int total = 0;
        for (IDriveHost drive : connectedDrives) {
            for (ICellInventory cell : drive.getActiveCellInventories()) {
                total += cell.getMaxBytes();
            }
        }
        this.cachedTotalCapacity = total;
        return total;
    }

    @Override
    public int getTotalUsedBytes() {
        if (cachedTotalUsed != -1) {
            return cachedTotalUsed;
        }

        int total = 0;
        for (IDriveHost drive : connectedDrives) {
            for (ICellInventory cell : drive.getActiveCellInventories()) {
                total += cell.getUsedBytes();
            }
        }
        this.cachedTotalUsed = total;
        return total;
    }

    @Override
    public List<ItemStack> getAllItems() {
        if (cachedVirtualPool != null) {
            return cachedVirtualPool;
        }

        Map<ItemKey, ItemStack> aggregatedMap = new LinkedHashMap<>();

        for (IDriveHost drive : connectedDrives) {
            for (ICellInventory cell : drive.getActiveCellInventories()) {
                for (ItemStack stack : cell.getStoredItems()) {
                    if (!stack.isEmpty()) {
                        ItemKey key = new ItemKey(stack);
                        ItemStack existing = aggregatedMap.get(key);
                        if (existing == null) {
                            aggregatedMap.put(key, stack.copy());
                        } else {
                            existing.grow(stack.getCount());
                        }
                    }
                }
            }
        }

        this.cachedVirtualPool = new ArrayList<>(aggregatedMap.values());
        return Collections.unmodifiableList(this.cachedVirtualPool);
    }

    @Override
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Перевіряємо глобальний ліміт пам'яті мережі
        int freeBytes = getRemainingBytes();
        if (freeBytes <= 0) {
            return stack.copy();
        }

        // Максимум, скільки ми можемо прийняти взагалі
        int canAcceptTotal = Math.min(stack.getCount(), freeBytes);
        ItemStack toDistribute = stack.copy();
        toDistribute.setCount(canAcceptTotal);

        // Решта, яка гарантовано не влізе через брак байт
        ItemStack unacceptedOverflow = stack.copy();
        unacceptedOverflow.shrink(canAcceptTotal);

        // Накопичувачі, що приймають предмет (білий список), згруповані за пріоритетом: вищий — першим.
        java.util.TreeMap<Integer, List<ICellInventory>> tiers = new java.util.TreeMap<>(Collections.reverseOrder());
        for (IDriveHost drive : connectedDrives) {
            if (drive.acceptsItem(toDistribute)) {
                tiers.computeIfAbsent(drive.getPriority(), k -> new ArrayList<>())
                        .addAll(drive.getActiveCellInventories());
            }
        }

        for (List<ICellInventory> allCells : tiers.values()) {
            if (toDistribute.isEmpty()) break;

            // Усередині рівня: 1-й прохід — диски, де ЦЕЙ предмет уже зберігається (дефрагментація)
            for (ICellInventory cell : allCells) {
                if (toDistribute.isEmpty()) break;
                if (hasItem(cell, toDistribute)) {
                    toDistribute = cell.insertItem(toDistribute, simulate);
                }
            }

            // 2-й прохід: будь-які диски рівня з вільними байтами
            if (!toDistribute.isEmpty()) {
                for (ICellInventory cell : allCells) {
                    if (toDistribute.isEmpty()) break;
                    if (!cell.isFull()) {
                        toDistribute = cell.insertItem(toDistribute, simulate);
                    }
                }
            }
        }

        if (!simulate) {
            invalidateCache();
            markDrivesDirty();
        }

        // Сумарний залишок = те, що не помістилося в диски + початковий оверфлоу
        int finalRemainderCount = toDistribute.getCount() + unacceptedOverflow.getCount();
        if (finalRemainderCount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack finalRemainder = stack.copy();
        finalRemainder.setCount(finalRemainderCount);
        return finalRemainder;
    }

    @Override
    public ItemStack extractItem(ItemStack template, int count, boolean simulate) {
        if (template.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }

        int needed = count;
        ItemStack result = ItemStack.EMPTY;

        for (IDriveHost drive : connectedDrives) {
            if (needed <= 0) break;

            for (ICellInventory cell : drive.getActiveCellInventories()) {
                if (needed <= 0) break;

                ItemStack extracted = cell.extractItem(template, needed, simulate);
                if (!extracted.isEmpty()) {
                    if (result.isEmpty()) {
                        result = extracted.copy();
                    } else {
                        result.grow(extracted.getCount());
                    }
                    needed -= extracted.getCount();
                }
            }
        }

        if (!simulate && !result.isEmpty()) {
            invalidateCache();
            markDrivesDirty();
        }

        return result;
    }

    @Override
    public ItemStack extractItem(Item item, int count, boolean simulate) {
        if (item == null || count <= 0) {
            return ItemStack.EMPTY;
        }

        // Знаходимо перший відповідний предмет у віртуальному пулі
        for (ItemStack stored : getAllItems()) {
            if (stored.is(item)) {
                return extractItem(stored, count, simulate);
            }
        }

        return ItemStack.EMPTY;
    }

    private void markDrivesDirty() {
        for (IDriveHost drive : connectedDrives) {
            drive.onCellContentsChanged();
        }
    }

    private boolean hasItem(ICellInventory cell, ItemStack match) {
        for (ItemStack item : cell.getStoredItems()) {
            if (ItemStack.isSameItemSameTags(item, match)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Допоміжний клас-ключ для швидкого порівняння предметів з урахуванням NBT.
     */
    private static class ItemKey {
        private final Item item;
        private final net.minecraft.nbt.CompoundTag tag;

        public ItemKey(ItemStack stack) {
            this.item = stack.getItem();
            this.tag = stack.getTag() != null ? stack.getTag().copy() : null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemKey itemKey = (ItemKey) o;
            return Objects.equals(item, itemKey.item) && Objects.equals(tag, itemKey.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, tag);
        }
    }
}
