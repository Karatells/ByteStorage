package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.dimension.block.entity.QuantumRelayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Квантовое реле сети.
 */
public class QuantumRelayBlock extends BaseEntityBlock {

    public QuantumRelayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumRelayBlockEntity(pos, state);
    }
}
