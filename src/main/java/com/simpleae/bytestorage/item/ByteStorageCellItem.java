package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.api.storage.CellTier;
import com.simpleae.bytestorage.api.storage.IByteStorageCell;
import com.simpleae.bytestorage.api.storage.ICellInventory;
import com.simpleae.bytestorage.storage.ByteCellInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Клас предмета диска зберігання (Storage Cell).
 * 
 * Підтримує різні об'єми (1k, 4k, 16k, 64k байт).
 * Реалізує відтворення смужки заповненості (item bar) та інформативних тултіпів.
 */
public class ByteStorageCellItem extends Item implements IByteStorageCell {

    private final CellTier tier;
    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    public ByteStorageCellItem(CellTier tier, Properties properties) {
        super(properties.stacksTo(1)); // Диски зберігання не стакуються
        this.tier = tier;
    }

    @Override
    public CellTier getTier(ItemStack stack) {
        return tier;
    }

    @Override
    public int getMaxBytes(ItemStack stack) {
        return tier.getCapacityBytes();
    }

    @Override
    public int getUsedBytes(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ByteCellInventory.TAG_USED_BYTES)) {
            return tag.getInt(ByteCellInventory.TAG_USED_BYTES);
        }
        return 0;
    }

    @Override
    public ICellInventory getCellInventory(ItemStack stack) {
        return new ByteCellInventory(stack, tier);
    }

    // --- Відображення смужки заповненості диска (Durability / Item Bar) ---

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getUsedBytes(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int used = getUsedBytes(stack);
        int max = getMaxBytes(stack);
        return Math.min(13, Math.round(13.0F * (float) used / (float) max));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float ratio = (float) getUsedBytes(stack) / (float) getMaxBytes(stack);
        if (ratio >= 1.0F) {
            return 0xFF0000; // Червоний (повний)
        } else if (ratio >= 0.75F) {
            return 0xFFA500; // Помаранчевий (майже повний)
        } else if (ratio >= 0.50F) {
            return 0xFFFF00; // Жовтий (напівзаповнений)
        }
        return 0x00FF00; // Зелений
    }

    // --- Інформативні спливаючі підказки (Tooltip) ---

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        int used = getUsedBytes(stack);
        int max = getMaxBytes(stack);
        double percent = ((double) used / max) * 100.0;

        // Рівень диска
        tooltip.add(Component.literal("Тип ємності: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(tier.getName().toUpperCase() + " (" + FORMAT.format(max) + " Б)")
                        .withStyle(ChatFormatting.AQUA)));

        // Використана пам'ять
        ChatFormatting statusColor = used >= max ? ChatFormatting.RED : (used > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
        tooltip.add(Component.literal("Зайнято пам'яті: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(FORMAT.format(used) + " / " + FORMAT.format(max) + " байт (" + String.format(Locale.ROOT, "%.1f", percent) + "%)")
                        .withStyle(statusColor)));

        // Підказка про логіку
        tooltip.add(Component.literal("Механіка: 1 предмет = 1 байт | Без ліміту на типи")
                .withStyle(ChatFormatting.DARK_AQUA));

        // Перегляд вмісту при затиснутому Shift
        ICellInventory inv = getCellInventory(stack);
        List<ItemStack> items = inv.getStoredItems();

        if (items.isEmpty()) {
            tooltip.add(Component.literal("Диск порожній")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.literal("Унікальних типів: " + items.size())
                    .withStyle(ChatFormatting.GRAY));

            if (Screen.hasShiftDown()) {
                tooltip.add(Component.literal("Вміст диска:").withStyle(ChatFormatting.YELLOW));
                int limit = Math.min(items.size(), 8);
                for (int i = 0; i < limit; i++) {
                    ItemStack stored = items.get(i);
                    tooltip.add(Component.literal(" • ")
                            .withStyle(ChatFormatting.DARK_GRAY)
                            .append(stored.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" x" + FORMAT.format(stored.getCount())).withStyle(ChatFormatting.GOLD)));
                }
                if (items.size() > limit) {
                    tooltip.add(Component.literal(" ...ще " + (items.size() - limit) + " типів...")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            } else {
                tooltip.add(Component.literal("Затисніть [Shift] для перегляду вмісту")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
    }
}
