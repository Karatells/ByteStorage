package com.simpleae.bytestorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Карта уничтожения излишков: при заполнении накопителя с фильтром лишние отфильтрованные предметы уничтожаются. */
public class VoidCardItem extends Item {

    public VoidCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.simple_bytestorage.void_card")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
