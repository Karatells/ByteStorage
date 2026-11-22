package com.simpleae.bytestorage.dimension.world;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.item.CapacitorAccumulatorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Уникальные эффекты среды в биомах измерения «Кремниевый План»:
 * - Битые Сектора (corrupted_badlands): Десинхронизация (помехи + лёгкий сдвиг), блокируется Стабилизатором фазы.
 * - Крио-Кэш (cryo_cache): мягкое обморожение, блокируется Стабилизатором фазы.
 * - Поля Конденсаторов (capacitor_flats): электростатическое ускорение (Speed I).
 * - Парящий Архив (cloud_archive): сниженная гравитация (Slow Falling).
 */
public class SiliconEnvironmentHandler {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        if (!SiliconPlaneDimension.isSiliconPlane(level)) return;

        long time = level.getGameTime();
        if (time % 40 != 0) return; // Проверяем раз в 2 секунды

        BlockPos pos = player.blockPosition();
        Holder<Biome> biome = level.getBiome(pos);
        boolean survivalLike = !player.isCreative();
        boolean stabilized = hasPhaseStabilizer(player);

        if (biome.is(SiliconPlaneDimension.BIOME_CORRUPTED_BADLANDS)) {
            if (survivalLike && !stabilized && time % 120 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 70, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, true, false));
                level.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        24, 0.5, 0.7, 0.5, 0.12);
                level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.AMBIENT, 0.55F, 1.6F);
            }
        } else if (biome.is(SiliconPlaneDimension.BIOME_CRYO_CACHE)) {
            if (survivalLike && !stabilized) {
                player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze(), player.getTicksFrozen() + 28));
            }
        } else if (biome.is(SiliconPlaneDimension.BIOME_CAPACITOR_FLATS)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 0.8, player.getZ(),
                    6, 0.5, 0.5, 0.5, 0.04);
            // Раз в ~18 секунд в Полях Конденсаторов бьёт разряд молнии; Аккумулятор перехватывает +50 000 FE!
            if (time % 360 == 0) {
                ItemStack acc = findAccumulator(player);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    if (!acc.isEmpty()) {
                        bolt.moveTo(player.getX(), player.getY(), player.getZ());
                        bolt.setVisualOnly(true);
                        level.addFreshEntity(bolt);
                        int added = CapacitorAccumulatorItem.addEnergy(acc, 50_000);
                        if (added > 0) {
                            player.displayClientMessage(Component.translatable(
                                    "msg.simple_bytestorage_dimension.accumulator.captured",
                                    added
                            ).withStyle(ChatFormatting.GOLD), true);
                        }
                    } else {
                        double lx = player.getX() + (level.random.nextDouble() - 0.5) * 16.0;
                        double lz = player.getZ() + (level.random.nextDouble() - 0.5) * 16.0;
                        bolt.moveTo(lx, player.getY(), lz);
                        bolt.setVisualOnly(true);
                        level.addFreshEntity(bolt);
                    }
                }
            }
        } else if (biome.is(SiliconPlaneDimension.BIOME_CLOUD_ARCHIVE)) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false));
        }
    }

    private static ItemStack findAccumulator(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.is(SimpleAeDimension.CAPACITOR_ACCUMULATOR.get())
                    && CapacitorAccumulatorItem.getEnergy(st) < CapacitorAccumulatorItem.MAX_ENERGY) {
                return st;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasPhaseStabilizer(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.is(SimpleAeDimension.PHASE_STABILIZER.get())) {
                return true;
            }
        }
        return false;
    }
}
