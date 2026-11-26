package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.dimension.DimensionConfig;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.block.entity.RiftGateCoreBlockEntity;
import com.simpleae.bytestorage.dimension.portal.RiftFrameHelper;
import com.simpleae.bytestorage.dimension.world.SiliconPlaneDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ядро Врат Разлома (Rift Gate Core).
 * В Обычном мире может быть активировано ТОЛЬКО внутри биома simple_bytestorage:silicon_rift
 * при наличии достаточного заряда FE (100 000 FE по умолчанию) и ключа (Corrupted Cell или Rift Resonator).
 * При попытке активации вне Silicon Rift выдаёт игроку смешную ачивку «Сигнал не ловит».
 */
public class RiftGateCoreBlock extends Block implements EntityBlock {

    public static final ResourceLocation ADV_WRONG_BIOME =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "wrong_biome_signal");
    public static final ResourceLocation ADV_OPEN_GATE =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "open_rift_gate");

    public RiftGateCoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RiftGateCoreBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RiftGateCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean isCorruptedCell = held.is(SimpleByteStorage.CORRUPTED_CELL.get());
        boolean isResonator = held.is(SimpleAeDimension.RIFT_RESONATOR.get());

        // Пустой рукой или другим предметом — показать статус ядра
        if (!isCorruptedCell && !isResonator) {
            boolean biomeOk = SiliconPlaneDimension.isSiliconPlane(level)
                    || !DimensionConfig.REQUIRE_SILICON_RIFT_BIOME.get()
                    || SiliconPlaneDimension.isInSiliconRiftBiome(level, pos);
            boolean frameOk = RiftFrameHelper.findValidFrame(level, pos) != null;
            player.displayClientMessage(Component.translatable(
                    "msg.simple_bytestorage_dimension.gate.status",
                    core.getEnergyStored(),
                    core.getRequiredEnergy(),
                    Component.translatable(frameOk
                            ? "msg.simple_bytestorage_dimension.ok"
                            : "msg.simple_bytestorage_dimension.no"),
                    Component.translatable(biomeOk
                            ? "msg.simple_bytestorage_dimension.ok"
                            : "msg.simple_bytestorage_dimension.wrong_biome_short")
            ).withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }

        if (!DimensionConfig.ENABLE_DIMENSION.get()) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage_dimension.gate.disabled")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        RiftFrameHelper.Frame frame = RiftFrameHelper.findValidFrame(level, pos);
        if (frame == null) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage_dimension.gate.no_frame")
                    .withStyle(ChatFormatting.YELLOW), false);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.BLOCKS, 0.8F, 0.6F);
            return InteractionResult.CONSUME;
        }

        // Проверка биома Silicon Rift (для Overworld и любых измерений кроме самого Silicon Plane)
        if (!SiliconPlaneDimension.isSiliconPlane(level)
                && DimensionConfig.REQUIRE_SILICON_RIFT_BIOME.get()
                && !SiliconPlaneDimension.isInSiliconRiftBiome(level, pos)) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage_dimension.gate.wrong_biome")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0F, 0.7F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        18, 0.35, 0.35, 0.35, 0.03);
            }
            if (player instanceof ServerPlayer sp) {
                awardAdvancement(sp, ADV_WRONG_BIOME);
            }
            return InteractionResult.CONSUME;
        }

        // Проверка энергии (в Silicon Plane для возврата домой энергия не требуется, если игрок застрял)
        boolean needEnergy = !SiliconPlaneDimension.isSiliconPlane(level);
        if (needEnergy && !core.hasEnoughEnergy()) {
            player.displayClientMessage(Component.translatable(
                    "msg.simple_bytestorage_dimension.gate.no_energy",
                    core.getEnergyStored(), core.getRequiredEnergy()
            ).withStyle(ChatFormatting.RED), false);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.BLOCKS, 0.8F, 0.5F);
            return InteractionResult.CONSUME;
        }

        if (needEnergy) {
            core.consumeActivationEnergy();
        }
        if (isCorruptedCell && !player.getAbilities().instabuild) {
            held.shrink(1);
        }

        frame.fillWithPortal(level);
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.75F, 1.35F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    32, 0.7, 1.0, 0.7, 0.05);
        }
        player.displayClientMessage(Component.translatable("msg.simple_bytestorage_dimension.gate.opened")
                .withStyle(ChatFormatting.AQUA), true);
        if (player instanceof ServerPlayer sp) {
            awardAdvancement(sp, ADV_OPEN_GATE);
        }
        return InteractionResult.CONSUME;
    }

    private static void awardAdvancement(ServerPlayer player, ResourceLocation id) {
        Advancement adv = player.server.getAdvancements().getAdvancement(id);
        if (adv != null) {
            var progress = player.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(adv, criterion);
                }
            }
        }
    }
}
