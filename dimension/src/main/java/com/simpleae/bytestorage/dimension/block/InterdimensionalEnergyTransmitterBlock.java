package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.block.entity.InterdimensionalEnergyTransmitterBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Межпространственный передатчик энергии.
 * - ПКМ: показать канал, заряд и число связанных узлов.
 * - Shift+ПКМ: переключить канал (1–16).
 */
public class InterdimensionalEnergyTransmitterBlock extends BaseEntityBlock {

    public InterdimensionalEnergyTransmitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InterdimensionalEnergyTransmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, SimpleAeDimension.ENERGY_TRANSMITTER_ENTITY.get(),
                        InterdimensionalEnergyTransmitterBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof InterdimensionalEnergyTransmitterBlockEntity be) {
            if (player.isShiftKeyDown()) {
                int ch = be.cycleChannel();
                player.displayClientMessage(Component.translatable(
                        "msg.simple_bytestorage_dimension.energy_transmitter.channel", ch
                ).withStyle(ChatFormatting.GOLD), true);
            } else {
                player.displayClientMessage(Component.translatable(
                        "msg.simple_bytestorage_dimension.energy_transmitter.status",
                        be.getChannel(),
                        String.format("%,d", be.getStoredEnergy()),
                        String.format("%,d", InterdimensionalEnergyTransmitterBlockEntity.CAPACITY),
                        be.getLinkedCount()
                ).withStyle(ChatFormatting.AQUA), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
