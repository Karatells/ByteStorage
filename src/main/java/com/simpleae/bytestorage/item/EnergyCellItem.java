package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.api.storage.CellTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Энергетическая байт-ячейка: хранит энергию FE по правилу 1 байт = 1 000 FE.
 * Устанавливается в обычный Drive, автоматически подпитывает Контроллер ниже 50% и заряжается выше 90%.
 */
public class EnergyCellItem extends Item {

    public static final String TAG_ENERGY = "StoredFE";
    public static final int FE_PER_BYTE = 1_000;
    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    private final CellTier tier;

    public EnergyCellItem(CellTier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public CellTier getTier() {
        return tier;
    }

    public int getMaxEnergy() {
        long cap = (long) tier.getCapacityBytes() * FE_PER_BYTE;
        return (int) Math.min(Integer.MAX_VALUE, cap);
    }

    public int getStoredEnergy(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, Math.min(getMaxEnergy(), stack.getTag().getInt(TAG_ENERGY))) : 0;
    }

    public void setStoredEnergy(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt(TAG_ENERGY, Math.max(0, Math.min(getMaxEnergy(), amount)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        float ratio = (float) getStoredEnergy(stack) / Math.max(1, getMaxEnergy());
        return Math.round(13.0F * Mth.clamp(ratio, 0.0F, 1.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFB020;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.simple_bytestorage.energy_cell",
                FORMAT.format(getStoredEnergy(stack)), FORMAT.format(getMaxEnergy()))
                .withStyle(ChatFormatting.GOLD));
    }
}
