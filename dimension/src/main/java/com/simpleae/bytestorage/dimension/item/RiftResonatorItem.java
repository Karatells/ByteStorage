package com.simpleae.bytestorage.dimension.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Резонатор Разлома — многоразовый ключ для активации Врат Разлома (альтернатива одноразовой Corrupted Cell).
 */
public class RiftResonatorItem extends Item {

    public RiftResonatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.rift_resonator")
                .withStyle(ChatFormatting.AQUA));
    }
}
