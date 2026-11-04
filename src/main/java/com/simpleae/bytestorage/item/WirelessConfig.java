package com.simpleae.bytestorage.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/** Настройки Беспроводного терминала (улучшения, фильтр, режимы), хранятся в NBT терминала. */
public final class WirelessConfig {

    public static final int UPGRADE_SLOTS = 8;
    public static final int FILTER_SLOTS = 18;
    public static final int TOTAL_SLOTS = UPGRADE_SLOTS + FILTER_SLOTS;
    public static final int MACRO_MAX_LENGTH = 32;

    private static final String TAG = "Config";
    private static final String TAG_MACRO = "Macro";

    private WirelessConfig() {
    }

    /** Обработчик: слоты 0..7 — улучшения, 8..25 — фильтр (призраки). */
    public static ItemStackHandler load(ItemStack terminal) {
        ItemStackHandler result = new ItemStackHandler(TOTAL_SLOTS);
        CompoundTag tag = terminal.getTag();
        if (tag == null || !tag.contains(TAG)) {
            return result;
        }
        ItemStackHandler saved = new ItemStackHandler(1);
        saved.deserializeNBT(tag.getCompound(TAG));
        int size = saved.getSlots();
        if (size == TOTAL_SLOTS) {
            return saved;
        }
        // Старый формат (до 1.1.1.x): 2 слота улучшений + 9 слотов фильтра
        int oldUpgrades = 2;
        for (int i = 0; i < size; i++) {
            int target = i < oldUpgrades ? i : UPGRADE_SLOTS + (i - oldUpgrades);
            if (target < TOTAL_SLOTS) {
                result.setStackInSlot(target, saved.getStackInSlot(i));
            }
        }
        return result;
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

    public static boolean hasUpgrade(ItemStack terminal, WirelessUpgradeItem.Type type) {
        return hasUpgrade(load(terminal), type, -1);
    }

    /** Есть ли в обработчике улучшение данного вида (слот {@code except} не учитывается). */
    public static boolean hasUpgrade(ItemStackHandler handler, WirelessUpgradeItem.Type type, int except) {
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (i != except && handler.getStackInSlot(i).getItem() instanceof WirelessUpgradeItem u && u.getType() == type) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAutoTransfer(ItemStack terminal) {
        return hasUpgrade(terminal, WirelessUpgradeItem.Type.AUTO_TRANSFER);
    }

    public static String macroName(ItemStack terminal) {
        CompoundTag tag = terminal.getTag();
        return tag != null ? tag.getString(TAG_MACRO) : "";
    }

    public static void setMacroName(ItemStack terminal, String name) {
        terminal.getOrCreateTag().putString(TAG_MACRO, sanitizeMacro(name));
    }

    public static String sanitizeMacro(String raw) {
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                sb.append(c);
            }
            if (sb.length() >= MACRO_MAX_LENGTH) break;
        }
        return sb.toString();
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
