package com.simpleae.bytestorage.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/** Настройки Беспроводного терминала (улучшения, фильтр, режимы), хранятся в NBT терминала. */
public final class WirelessConfig {

    public static final int UPGRADE_SLOTS = 2;
    public static final int FILTER_SLOTS = 9;
    public static final int TOTAL_SLOTS = UPGRADE_SLOTS + FILTER_SLOTS;

    private static final String TAG = "Config";

    private WirelessConfig() {
    }

    /** Обработчик: слоты 0..1 — улучшения, 2..10 — фильтр (призраки). */
    public static ItemStackHandler load(ItemStack terminal) {
        ItemStackHandler handler = new ItemStackHandler(TOTAL_SLOTS);
        CompoundTag tag = terminal.getTag();
        if (tag != null && tag.contains(TAG)) {
            handler.deserializeNBT(tag.getCompound(TAG));
            if (handler.getSlots() != TOTAL_SLOTS) {
                ItemStackHandler fixed = new ItemStackHandler(TOTAL_SLOTS);
                for (int i = 0; i < Math.min(TOTAL_SLOTS, handler.getSlots()); i++) {
                    fixed.setStackInSlot(i, handler.getStackInSlot(i));
                }
                return fixed;
            }
        }
        return handler;
    }

    public static void save(ItemStack terminal, ItemStackHandler handler) {
        terminal.getOrCreateTag().put(TAG, handler.serializeNBT());
    }

    public static boolean getFlag(ItemStack terminal, String key, boolean def) {
        CompoundTag tag = terminal.getTag();
        return tag != null && tag.contains(key) ? tag.getBoolean(key) : def;
    }

    public static void setFlag(ItemStack terminal, String key, boolean value) {
        terminal.getOrCreateTag().putBoolean(key, value);
    }

    public static boolean restock(ItemStack t) {
        return getFlag(t, "Restock", true);
    }

    public static boolean pickup(ItemStack t) {
        return getFlag(t, "Pickup", true);
    }

    public static boolean whitelist(ItemStack t) {
        return getFlag(t, "Whitelist", false);
    }

    public static boolean hasAutoTransfer(ItemStack terminal) {
        ItemStackHandler h = load(terminal);
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (h.getStackInSlot(i).getItem() instanceof WirelessUpgradeItem) {
                return true;
            }
        }
        return false;
    }

    /** Пропускает ли фильтр предмет: пустой фильтр пропускает всё; белый список — только перечисленное; чёрный — всё кроме. */
    public static boolean allows(ItemStack terminal, ItemStack stack) {
        ItemStackHandler h = load(terminal);
        boolean any = false;
        boolean match = false;
        for (int i = UPGRADE_SLOTS; i < TOTAL_SLOTS; i++) {
            ItemStack f = h.getStackInSlot(i);
            if (f.isEmpty()) continue;
            any = true;
            if (ItemStack.isSameItemSameTags(f, stack)) {
                match = true;
            }
        }
        if (!any) return true;
        return whitelist(terminal) == match;
    }
}
