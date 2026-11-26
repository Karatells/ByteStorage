package com.simpleae.bytestorage.dimension.item;

import com.simpleae.bytestorage.dimension.block.entity.RiftGateCoreBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Аккумулятор конденсатора (500 000 FE):
 * - Автоматически перехватывает удары молний в Полях Конденсаторов (+50 000 FE за разряд!).
 * - ПКМ по Ядру Врат Разлома или любому FE-блоку мгновенно передаёт накопленную энергию.
 */
public class CapacitorAccumulatorItem extends Item {

    public static final String TAG_ENERGY = "StoredFE";
    public static final int MAX_ENERGY = 500_000;
    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    public CapacitorAccumulatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int getEnergy(ItemStack stack) {
        return stack.hasTag() ? Mth.clamp(stack.getTag().getInt(TAG_ENERGY), 0, MAX_ENERGY) : 0;
    }

    public static int addEnergy(ItemStack stack, int amount) {
        int cur = getEnergy(stack);
        int added = Math.min(amount, MAX_ENERGY - cur);
        if (added > 0) {
            stack.getOrCreateTag().putInt(TAG_ENERGY, cur + added);
        }
        return added;
    }

    public static int extractEnergy(ItemStack stack, int amount) {
        int cur = getEnergy(stack);
        int taken = Math.min(amount, cur);
        if (taken > 0) {
            stack.getOrCreateTag().putInt(TAG_ENERGY, cur - taken);
        }
        return taken;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();
        int stored = getEnergy(stack);
        if (stored <= 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            var be = level.getBlockEntity(pos);
            if (be != null) {
                var cap = be.getCapability(ForgeCapabilities.ENERGY, ctx.getClickedFace());
                if (cap.isPresent()) {
                    var target = cap.orElseThrow(IllegalStateException::new);
                    int moved = 0;
                    // Передаём энергию пакетами (RiftGateCore принимает по 5000 FE за вызов)
                    for (int i = 0; i < 40 && getEnergy(stack) > 0; i++) {
                        int accepted = target.receiveEnergy(Math.min(25_000, getEnergy(stack)), false);
                        if (accepted <= 0) break;
                        extractEnergy(stack, accepted);
                        moved += accepted;
                    }
                    if (moved > 0 && ctx.getPlayer() != null) {
                        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.4F);
                        ctx.getPlayer().displayClientMessage(Component.translatable(
                                "msg.simple_bytestorage_dimension.accumulator.transferred",
                                FORMAT.format(moved), FORMAT.format(getEnergy(stack))
                        ).withStyle(ChatFormatting.GOLD), true);
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / (float) MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFCC33;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.simple_bytestorage_dimension.accumulator.energy",
                FORMAT.format(getEnergy(stack)), FORMAT.format(MAX_ENERGY)
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.accumulator.tip")
                .withStyle(ChatFormatting.GRAY));
    }
}
