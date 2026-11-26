package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.dimension.world.SiliconPlaneDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Маяк возврата (Return Beacon) — точка возрождения в Кремниевом Плане (где обычные кровати не работают).
 */
public class ReturnBeaconBlock extends Block {

    public ReturnBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            if (!SiliconPlaneDimension.isSiliconPlane(level)) {
                sp.displayClientMessage(Component.translatable("msg.simple_bytestorage_dimension.beacon.wrong_dim")
                        .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.CONSUME;
            }
            sp.setRespawnPosition(level.dimension(), pos.above(), 0.0F, true, false);
            level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        20, 0.35, 0.4, 0.35, 0.04);
            }
            sp.displayClientMessage(Component.translatable(
                    "msg.simple_bytestorage_dimension.beacon.bound",
                    pos.getX(), pos.getY() + 1, pos.getZ()
            ).withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.CONSUME;
    }
}
