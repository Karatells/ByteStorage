package com.simpleae.bytestorage.dimension.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Фрагмент лора Архива — хранит случайную расшифрованную запись о Кремниевом Плане (5 записей).
 */
public class ArchiveLoreItem extends Item {

    private static final String TAG_ENTRY = "LoreEntry";
    private static final int ENTRY_COUNT = 5;

    public ArchiveLoreItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int entry;
            if (stack.hasTag() && stack.getTag().contains(TAG_ENTRY)) {
                entry = Math.floorMod(stack.getTag().getInt(TAG_ENTRY), ENTRY_COUNT);
            } else {
                entry = level.random.nextInt(ENTRY_COUNT);
                stack.getOrCreateTag().putInt(TAG_ENTRY, entry);
            }
            player.displayClientMessage(Component.translatable(
                    "lore.simple_bytestorage_dimension.entry." + (entry + 1)
            ).withStyle(ChatFormatting.AQUA), false);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.9F, 1.1F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.archive_lore")
                .withStyle(ChatFormatting.GRAY));
    }
}
