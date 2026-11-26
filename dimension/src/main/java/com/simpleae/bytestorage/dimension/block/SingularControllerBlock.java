package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.block.entity.SingularControllerBlockEntity;
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
 * Сингулярный контроллер (энерго- и вычислительное ядро сети).
 */
public class SingularControllerBlock extends BaseEntityBlock {

    public SingularControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SingularControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, SimpleAeDimension.SINGULAR_CONTROLLER_ENTITY.get(),
                        SingularControllerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SingularControllerBlockEntity be) {
            boolean online = be.getGrid() != null && be.getGrid().getState().isActive();
            player.displayClientMessage(Component.translatable(
                    "msg.simple_bytestorage_dimension.singular_controller.status",
                    String.format("%,d", be.getStoredEnergy()),
                    String.format("%,d", SingularControllerBlockEntity.CAPACITY),
                    online ? "ONLINE" : "STANDBY"
            ).withStyle(online ? ChatFormatting.AQUA : ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
