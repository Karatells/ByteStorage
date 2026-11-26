package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Светящаяся цифровая растительность Кремниевого Плана
 * (трава, цветы, кристальные кусты, плазменные стебли) — растёт на любых породах измерения.
 */
public class CyberPlantBlock extends BushBlock {

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public CyberPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(SimpleAeDimension.SILICON_SLATE_BLOCK.get())
                || state.is(SimpleAeDimension.SILICON_BASALT_BLOCK.get())
                || state.is(SimpleAeDimension.CRYO_BASALT_BLOCK.get())
                || state.is(SimpleAeDimension.CORRUPTED_STONE_BLOCK.get())
                || state.is(SimpleAeDimension.ARCHIVE_PLATE_BLOCK.get())
                || state.is(SimpleAeDimension.REGISTRY_OBSIDIAN_BLOCK.get())
                || state.isSolidRender(level, pos);
    }
}
