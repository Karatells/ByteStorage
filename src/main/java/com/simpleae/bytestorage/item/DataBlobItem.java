package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.SimpleByteStorage;
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

/** Стиснутий блок даних (64 предмети = 1 байт). ПКМ у руках розпаковує 1 блок назад у 64 предмети. */
public class DataBlobItem extends Item {

    public static final String TAG_TARGET = "Target";
    public static final String TAG_COUNT = "BlobCount";
    public static final int DEFAULT_RATIO = 64;

    public DataBlobItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createFor(ItemStack target, int count) {
        ItemStack blob = new ItemStack(SimpleByteStorage.DATA_BLOB.get());
        ItemStack single = target.copy();
        single.setCount(1);
        CompoundTag tag = blob.getOrCreateTag();
        tag.put(TAG_TARGET, single.save(new CompoundTag()));
        tag.putInt(TAG_COUNT, count);
        return blob;
    }

    public static ItemStack getTarget(ItemStack blob) {
        CompoundTag tag = blob.getTag();
        if (tag == null || !tag.contains(TAG_TARGET)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(tag.getCompound(TAG_TARGET));
    }

    public static int getBlobCount(ItemStack blob) {
        CompoundTag tag = blob.getTag();
        return tag != null && tag.contains(TAG_COUNT) ? Math.max(1, tag.getInt(TAG_COUNT)) : DEFAULT_RATIO;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack target = getTarget(held);
        if (target.isEmpty()) {
            return InteractionResultHolder.pass(held);
        }
        if (!level.isClientSide) {
            int count = getBlobCount(held);
            target.setCount(count);
            held.shrink(1);
            if (!player.getInventory().add(target)) {
                player.drop(target, false);
            }
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        ItemStack target = getTarget(stack);
        if (!target.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage.data_blob",
                    getBlobCount(stack), target.getHoverName()).withStyle(ChatFormatting.AQUA));
        }
    }
}
