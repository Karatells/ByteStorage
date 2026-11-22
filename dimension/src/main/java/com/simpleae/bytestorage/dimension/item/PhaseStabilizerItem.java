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
 * Стабилизатор фазы — пассивный защитный модуль в инвентаре игрока:
 * даёт иммунитет к Десинхронизации в Битых Секторах и обморожению в Крио-Кэше.
 */
public class PhaseStabilizerItem extends Item {

    public PhaseStabilizerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.phase_stabilizer")
                .withStyle(ChatFormatting.AQUA));
    }
}
