package com.simpleae.bytestorage.dimension.item;

import com.simpleae.bytestorage.item.SpeedCardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Улучшенная карта ускорения Mk2: даёт тройную мощность ускорения в слотах Асемблера.
 */
public class SpeedCardMk2Item extends SpeedCardItem {

    public SpeedCardMk2Item(Properties properties) {
        super(properties);
    }

    @Override
    public int getSpeedPower() {
        return 3;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.speed_card_mk2")
                .withStyle(ChatFormatting.AQUA));
    }
}
