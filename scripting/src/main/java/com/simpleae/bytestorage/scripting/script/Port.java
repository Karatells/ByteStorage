package com.simpleae.bytestorage.scripting.script;

import com.simpleae.bytestorage.api.storage.INetworkStorage;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

/** Єдиний вигляд «джерела/приймача» предметів: сусідній інвентар або мережа. */
abstract class Port {

    abstract int slots();

    abstract ItemStack get(int slot);

    abstract ItemStack extract(int slot, int amount, boolean simulate);

    /** Вставка; повертає залишок, що не помістився. */
    abstract ItemStack insert(ItemStack stack, boolean simulate);

    static final class Handler extends Port {
        private final IItemHandler handler;

        Handler(IItemHandler handler) {
            this.handler = handler;
        }

        @Override
        int slots() {
            return handler.getSlots();
        }

        @Override
        ItemStack get(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        ItemStack extract(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }

        @Override
        ItemStack insert(ItemStack stack, boolean simulate) {
            return ItemHandlerHelper.insertItem(handler, stack, simulate);
        }
    }

    /** Кілька інвентарів як один: слоти йдуть підряд, вставка пробує по черзі. */
    static final class Multi extends Port {
        private final java.util.List<Port> parts;

        Multi(java.util.List<Port> parts) {
            this.parts = parts;
        }

        @Override
        int slots() {
            int total = 0;
            for (Port part : parts) {
                total += part.slots();
            }
            return total;
        }

        private Port partAt(int slot, int[] local) {
            int rest = slot;
            for (Port part : parts) {
                int n = part.slots();
                if (rest < n) {
                    local[0] = rest;
                    return part;
                }
                rest -= n;
            }
            return null;
        }

        @Override
        ItemStack get(int slot) {
            int[] local = new int[1];
            Port part = partAt(slot, local);
            return part == null ? ItemStack.EMPTY : part.get(local[0]);
        }

        @Override
        ItemStack extract(int slot, int amount, boolean simulate) {
            int[] local = new int[1];
            Port part = partAt(slot, local);
            return part == null ? ItemStack.EMPTY : part.extract(local[0], amount, simulate);
        }

        @Override
        ItemStack insert(ItemStack stack, boolean simulate) {
            ItemStack rest = stack;
            for (Port part : parts) {
                rest = part.insert(rest, simulate);
                if (rest.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
            return rest;
        }
    }

    static final class Network extends Port {
        private final INetworkStorage storage;
        private final List<ItemStack> snapshot;

        Network(INetworkStorage storage) {
            this.storage = storage;
            this.snapshot = new ArrayList<>();
            for (ItemStack stack : storage.getAllItems()) {
                if (!stack.isEmpty()) {
                    snapshot.add(stack.copy());
                }
            }
        }

        @Override
        int slots() {
            return snapshot.size();
        }

        @Override
        ItemStack get(int slot) {
            return snapshot.get(slot);
        }

        @Override
        ItemStack extract(int slot, int amount, boolean simulate) {
            ItemStack template = snapshot.get(slot);
            if (template.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return storage.extractItem(template, Math.min(amount, template.getCount()), simulate);
        }

        @Override
        ItemStack insert(ItemStack stack, boolean simulate) {
            return storage.insertItem(stack, simulate);
        }
    }

    /**
     * Переносить до {@code n} предметів (item == null — будь-які) з {@code from} у {@code to}.
     * Не перевищує залишок бюджету предметів. Повертає, скільки реально перенесено.
     */
    static int transfer(Port from, Port to, Item item, int n, Budget budget) {
        int moved = 0;
        int slots = from.slots();
        for (int slot = 0; slot < slots && moved < n && budget.items > 0; slot++) {
            ItemStack inSlot = from.get(slot);
            if (inSlot.isEmpty() || (item != null && inSlot.getItem() != item)) {
                continue;
            }
            int want = Math.min(Math.min(n - moved, budget.items), inSlot.getCount());
            ItemStack simulated = from.extract(slot, want, true);
            if (simulated.isEmpty()) {
                continue;
            }
            ItemStack remainder = to.insert(simulated.copy(), true);
            int fits = simulated.getCount() - remainder.getCount();
            if (fits <= 0) {
                continue;
            }
            ItemStack taken = from.extract(slot, fits, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack rest = to.insert(taken.copy(), false);
            if (!rest.isEmpty()) {
                from.insert(rest.copy(), false);
            }
            int done = taken.getCount() - rest.getCount();
            moved += done;
            budget.items -= done;
        }
        return moved;
    }
}
