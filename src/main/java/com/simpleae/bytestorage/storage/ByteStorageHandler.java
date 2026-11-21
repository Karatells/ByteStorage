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
                        ItemKey key = ItemKey.of(stack);
                        ItemStack existing = aggregatedMap.get(key);
                        if (existing == null) {
                            aggregatedMap.put(ItemKey.owned(stack), stack.copy());
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
            return voidsAny(stack) ? ItemStack.EMPTY : stack.copy();
        }

        // Максимум, скільки ми можемо прийняти взагалі
        int canAcceptTotal = Math.min(stack.getCount(), freeBytes);
        ItemStack toDistribute = stack.copy();
        toDistribute.setCount(canAcceptTotal);

        // Решта, яка гарантовано не влізе через брак байт
        ItemStack unacceptedOverflow = stack.copy();
        unacceptedOverflow.shrink(canAcceptTotal);

        // Накопичувачі, що приймають предмет (білий список), згруповані за пріоритетом: вищий — першим.
        TreeMap<Integer, List<IDriveHost>> tiers = new TreeMap<>(Collections.reverseOrder());
        for (IDriveHost drive : connectedDrives) {
            if (drive.acceptsItem(toDistribute)) {
                tiers.computeIfAbsent(drive.getPriority(), k -> new ArrayList<>()).add(drive);
            }
        }

        Set<IDriveHost> modifiedDrives = simulate ? null : new HashSet<>();

        for (List<IDriveHost> drivesInTier : tiers.values()) {
            if (toDistribute.isEmpty()) break;

            // Усередині рівня: 1-й прохід — диски, де ЦЕЙ предмет уже зберігається (дефрагментація)
            for (IDriveHost drive : drivesInTier) {
                if (toDistribute.isEmpty()) break;
                for (ICellInventory cell : drive.getActiveCellInventories()) {
                    if (toDistribute.isEmpty()) break;
                    if (cell.acceptsItem(toDistribute) && cell.containsItem(toDistribute)) {
                        int before = toDistribute.getCount();
                        toDistribute = cell.insertItem(toDistribute, simulate);
                        if (!simulate && toDistribute.getCount() < before) {
                            modifiedDrives.add(drive);
                        }
                    }
                }
            }

            // 2-й прохід: будь-які диски рівня з вільними байтами
            if (!toDistribute.isEmpty()) {
                for (IDriveHost drive : drivesInTier) {
                    if (toDistribute.isEmpty()) break;
                    for (ICellInventory cell : drive.getActiveCellInventories()) {
                        if (toDistribute.isEmpty()) break;
                        if (!cell.isFull() && cell.acceptsItem(toDistribute)) {
                            int before = toDistribute.getCount();
                            toDistribute = cell.insertItem(toDistribute, simulate);
                            if (!simulate && toDistribute.getCount() < before) {
                                modifiedDrives.add(drive);
                            }
                        }
                    }
                }
            }
        }

        int insertedCount = canAcceptTotal - toDistribute.getCount();
        if (!simulate && insertedCount > 0) {
            this.cachedVirtualPool = null;
            if (this.cachedTotalUsed != -1) {
                this.cachedTotalUsed += insertedCount;
            }
            markDrivesDirty(modifiedDrives);
        }

        // Сумарний залишок = те, що не помістилося в диски + початковий оверфлоу
        int finalRemainderCount = toDistribute.getCount() + unacceptedOverflow.getCount();
        if (finalRemainderCount <= 0 || voidsAny(stack)) {
            return ItemStack.EMPTY;
        }

        ItemStack finalRemainder = stack.copy();
        finalRemainder.setCount(finalRemainderCount);
        return finalRemainder;
    }

    // --- Резерв сировини під замовлення автокрафту ---

    private final com.simpleae.bytestorage.util.ReservationLedger ledger = new com.simpleae.bytestorage.util.ReservationLedger();

    @Override
    public void reserve(String orderId, java.util.Map<String, Integer> amounts) {
        ledger.reserve(orderId, amounts);
    }

    @Override
    public void releaseReservation(String orderId) {
        ledger.release(orderId);
    }

    @Override
    public java.util.Map<String, java.util.Map<String, Integer>> getReservationSnapshot() {
        return ledger.snapshot();
    }

    @Override
    public void restoreReservations(java.util.Map<String, java.util.Map<String, Integer>> data) {
        ledger.restore(data);
    }

    @Override
    public void clearReservations() {
        ledger.clear();
    }

    @Override
    public int getTotalReserved() {
        return ledger.totalReserved();
    }

    @Override
    public int getReserved(ItemStack stack) {
        return ledger.reserved(com.simpleae.bytestorage.util.CraftingService.key(stack));
    }

    /** Звичайне вилучення (термінали, шини, бездротовий) не може зачепити зарезервоване. */
    @Override
    public ItemStack extractItem(ItemStack template, int count, boolean simulate) {
        if (template.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        int reserved = ledger.isEmpty() ? 0 : getReserved(template);
        if (reserved > 0) {
            int total = 0;
            for (ItemStack stored : getAllItems()) {
                if (ItemStack.isSameItemSameTags(stored, template)) {
                    total = stored.getCount();
                    break;
                }
            }
            int free = total - reserved;
            if (free <= 0) {
                return ItemStack.EMPTY;
            }
            count = Math.min(count, free);
        }
        return extractRaw(template, count, simulate);
    }

    @Override
    public ItemStack extractForCrafting(ItemStack template, int count, boolean simulate) {
        ItemStack result = extractRaw(template, count, simulate);
        if (!simulate && !result.isEmpty() && !ledger.isEmpty()) {
            ledger.consume(com.simpleae.bytestorage.util.CraftingService.key(template), result.getCount());
        }
        return result;
    }

    private ItemStack extractRaw(ItemStack template, int count, boolean simulate) {
        if (template.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }

        int needed = count;
        ItemStack result = ItemStack.EMPTY;
        Set<IDriveHost> modifiedDrives = simulate ? null : new HashSet<>();

        // Витягуємо спочатку з накопичувачів з найнижчим пріоритетом, щоб буферні диски звільнялися першими.
        List<IDriveHost> orderedDrives = new ArrayList<>(connectedDrives);
        orderedDrives.sort(Comparator.comparingInt(IDriveHost::getPriority));

        for (IDriveHost drive : orderedDrives) {
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
                    if (!simulate) {
                        modifiedDrives.add(drive);
                    }
                }
            }
        }

        if (!simulate && !result.isEmpty()) {
            this.cachedVirtualPool = null;
            if (this.cachedTotalUsed != -1) {
                this.cachedTotalUsed = Math.max(0, this.cachedTotalUsed - result.getCount());
            }
            markDrivesDirty(modifiedDrives);
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

    private boolean voidsAny(ItemStack stack) {
        for (IDriveHost drive : connectedDrives) {
            if (drive.voidsOverflow(stack)) {
                return true;
            }
        }
        return false;
    }

    private void markDrivesDirty(Set<IDriveHost> drives) {
        for (IDriveHost drive : drives) {
            drive.onCellContentsChanged();
        }
    }

    /**
     * Допоміжний клас-ключ для швидкого порівняння предметів з урахуванням NBT.
     */
    private static class ItemKey {
        private final Item item;
        private final net.minecraft.nbt.CompoundTag tag;
        private final int hash;

        private ItemKey(Item item, net.minecraft.nbt.CompoundTag tag) {
            this.item = item;
            this.tag = tag;
            this.hash = Objects.hash(item, tag);
        }

        static ItemKey of(ItemStack stack) {
            return new ItemKey(stack.getItem(), stack.getTag());
        }

        static ItemKey owned(ItemStack stack) {
            net.minecraft.nbt.CompoundTag t = stack.getTag();
            return new ItemKey(stack.getItem(), t != null ? t.copy() : null);
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
            return hash;
        }
    }
}
