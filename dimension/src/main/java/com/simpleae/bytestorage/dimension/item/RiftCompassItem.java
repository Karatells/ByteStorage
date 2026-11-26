package com.simpleae.bytestorage.dimension.item;

import com.mojang.datafixers.util.Pair;
import com.simpleae.bytestorage.dimension.world.SiliconPlaneDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Компас Разлома:
 * - В Обычном мире ищет ближайший биом simple_bytestorage:silicon_rift (где можно открыть Врата Разлома).
 * - В Кремниевом Плане: Shift+ПКМ циклически переключает целевой биом (все 8 биомов), ПКМ ищет выбранный биом.
 */
public class RiftCompassItem extends Item {

    private static final String TAG_TARGET_IDX = "TargetBiomeIdx";

    public RiftCompassItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static int getTargetIdx(ItemStack stack) {
        int raw = stack.hasTag() ? stack.getTag().getInt(TAG_TARGET_IDX) : 1;
        int size = SiliconPlaneDimension.ALL_BIOMES.size();
        return ((raw % size) + size) % size;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            if (SiliconPlaneDimension.isSiliconPlane(sl) && player.isShiftKeyDown()) {
                int next = (getTargetIdx(stack) + 1) % SiliconPlaneDimension.ALL_BIOMES.size();
                stack.getOrCreateTag().putInt(TAG_TARGET_IDX, next);
                ResourceKey<Biome> selected = SiliconPlaneDimension.ALL_BIOMES.get(next);
                player.displayClientMessage(Component.translatable(
                        "msg.simple_bytestorage_dimension.compass.mode",
                        Component.translatable(SiliconPlaneDimension.translationKey(selected))
                ).withStyle(ChatFormatting.GOLD), true);
                sl.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(),
                        SoundSource.PLAYERS, 0.5F, 1.3F);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            ResourceKey<Biome> targetBiome = SiliconPlaneDimension.isSiliconPlane(sl)
                    ? SiliconPlaneDimension.ALL_BIOMES.get(getTargetIdx(stack))
                    : SiliconPlaneDimension.OVERWORLD_SILICON_RIFT;
            String biomeLabelKey = SiliconPlaneDimension.translationKey(targetBiome);

            Pair<BlockPos, Holder<Biome>> found = sl.findClosestBiome3d(
                    h -> h.is(targetBiome),
                    player.blockPosition(),
                    6400, 32, 64
            );
            if (found != null) {
                BlockPos p = found.getFirst();
                int dist = (int) Math.sqrt(player.blockPosition().distSqr(p));
                player.displayClientMessage(Component.translatable(
                        "msg.simple_bytestorage_dimension.compass.found",
                        Component.translatable(biomeLabelKey),
                        p.getX(), p.getZ(), dist
                ).withStyle(ChatFormatting.AQUA), false);
                sl.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS, 0.8F, 1.4F);
            } else {
                player.displayClientMessage(Component.translatable(
                        "msg.simple_bytestorage_dimension.compass.not_found",
                        Component.translatable(biomeLabelKey)
                ).withStyle(ChatFormatting.YELLOW), false);
            }
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.rift_compass")
                .withStyle(ChatFormatting.GRAY));
        ResourceKey<Biome> sel = SiliconPlaneDimension.ALL_BIOMES.get(getTargetIdx(stack));
        tooltip.add(Component.translatable(
                "msg.simple_bytestorage_dimension.compass.mode",
                Component.translatable(SiliconPlaneDimension.translationKey(sel))
        ).withStyle(ChatFormatting.AQUA));
    }
}
