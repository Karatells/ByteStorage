package com.simpleae.bytestorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** Запутаний кристал: Shift+ПКМ зі стаком від 2 шт. прив'язує пару для Квантового ретранслятора. */
public class EntangledCrystalItem extends Item {

    public static final String TAG_LINK_ID = "LinkId";

    public EntangledCrystalItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static UUID getLinkId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(TAG_LINK_ID)) {
            return tag.getUUID(TAG_LINK_ID);
        }
        return null;
    }

    public static void setLinkId(ItemStack stack, UUID id) {
        stack.getOrCreateTag().putUUID(TAG_LINK_ID, id);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.getCount() >= 2) {
            if (!level.isClientSide) {
                UUID id = UUID.randomUUID();
                setLinkId(stack, id);
                player.displayClientMessage(
                        Component.translatable("msg.simple_bytestorage.crystal.paired", id.toString().substring(0, 8))
                                .withStyle(ChatFormatting.AQUA),
                        true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getLinkId(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        UUID id = getLinkId(stack);
        if (id != null) {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage.crystal.linked", id.toString().substring(0, 8))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage.crystal.unlinked")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
