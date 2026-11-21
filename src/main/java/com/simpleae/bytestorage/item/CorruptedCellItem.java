package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.SimpleByteStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Повреждённая байт-ячейка из разлома Silicon Rift.
 * Восстанавливается в Cell Workbench (слот 0 + кнопка ->) в ценный предмет Simple Byte Storage.
 */
public class CorruptedCellItem extends Item {

    public CorruptedCellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Генерирует восстановленную награду из повреждённой ячейки. */
    public static ItemStack rollRestoredReward(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 20) return new ItemStack(SimpleByteStorage.CELL_4K.get());
        if (roll < 35) return new ItemStack(SimpleByteStorage.CELL_16K.get());
        if (roll < 48) return new ItemStack(SimpleByteStorage.FLUID_CELL_4K.get());
        if (roll < 60) return new ItemStack(SimpleByteStorage.ENERGY_CELL_1K.get());
        if (roll < 72) return new ItemStack(SimpleByteStorage.PORTABLE_CELL_1K.get());
        if (roll < 82) return new ItemStack(SimpleByteStorage.SPEED_CARD.get(), 2);
        if (roll < 89) return new ItemStack(SimpleByteStorage.VOID_CARD.get());
        if (roll < 95) return new ItemStack(SimpleByteStorage.LOCK_CARD.get());
        return new ItemStack(SimpleByteStorage.MAGNET_CARD.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.simple_bytestorage.corrupted_cell")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
