package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * Таблички и подвесные таблички из оптической древесины с собственными BlockEntity.
 */
public final class OpticSignBlocks {

    private OpticSignBlocks() {}

    public static class Standing extends StandingSignBlock {
        public Standing(Properties properties, WoodType type) {
            super(properties, type);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new OpticSignBlockEntity(pos, state);
        }
    }

    public static class Wall extends WallSignBlock {
        public Wall(Properties properties, WoodType type) {
            super(properties, type);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new OpticSignBlockEntity(pos, state);
        }
    }

    public static class CeilingHanging extends CeilingHangingSignBlock {
        public CeilingHanging(Properties properties, WoodType type) {
            super(properties, type);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new OpticHangingSignBlockEntity(pos, state);
        }
    }

    public static class WallHanging extends WallHangingSignBlock {
        public WallHanging(Properties properties, WoodType type) {
            super(properties, type);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new OpticHangingSignBlockEntity(pos, state);
        }
    }

    public static class OpticSignBlockEntity extends SignBlockEntity {
        public OpticSignBlockEntity(BlockPos pos, BlockState state) {
            super(SimpleAeDimension.OPTIC_SIGN_ENTITY.get(), pos, state);
        }

        @Override
        public BlockEntityType<?> getType() {
            return SimpleAeDimension.OPTIC_SIGN_ENTITY.get();
        }
    }

    public static class OpticHangingSignBlockEntity extends HangingSignBlockEntity {
        public OpticHangingSignBlockEntity(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public BlockEntityType<?> getType() {
            return SimpleAeDimension.OPTIC_HANGING_SIGN_ENTITY.get();
        }
    }
}
