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
 * - В Кремниевом Плане ищет ближайшие Магистральные Каньоны (bus_canyons) или Оптоволоконный Лес (Shift+ПКМ).
 */
public class RiftCompassItem extends Item {

    public RiftCompassItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            ResourceKey<Biome> targetBiome;
            String biomeLabelKey;
            if (SiliconPlaneDimension.isSiliconPlane(sl)) {
                if (player.isShiftKeyDown()) {
                    targetBiome = SiliconPlaneDimension.BIOME_OPTIC_SPIREWOOD;
                    biomeLabelKey = "biome.simple_bytestorage_dimension.optic_spirewood";
                } else {
                    targetBiome = SiliconPlaneDimension.BIOME_BUS_CANYONS;
                    biomeLabelKey = "biome.simple_bytestorage_dimension.bus_canyons";
                }
            } else {
                targetBiome = SiliconPlaneDimension.OVERWORLD_SILICON_RIFT;
                biomeLabelKey = "biome.simple_bytestorage.silicon_rift";
            }

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
            player.getCooldowns().addCooldown(this, 60);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.rift_compass")
                .withStyle(ChatFormatting.GRAY));
    }
}
