package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Саженец оптического дерева: вырастает в светящееся кибер-дерево из optic_log и optic_leaves
 * со временем или при применении костной муки.
 */
public class OpticSaplingBlock extends CyberPlantBlock implements BonemealableBlock {

    public OpticSaplingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) == 0) {
            growTree(level, pos, random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.65F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        growTree(level, pos, random);
    }

    private static void growTree(ServerLevel level, BlockPos origin, RandomSource random) {
        int height = 5 + random.nextInt(4);
        BlockState log = SimpleAeDimension.OPTIC_LOG_BLOCK.get().defaultBlockState();
        BlockState leaves = SimpleAeDimension.OPTIC_LEAVES_BLOCK.get().defaultBlockState();

        for (int y = 1; y <= height + 1; y++) {
            BlockPos check = origin.above(y);
            if (!level.getBlockState(check).isAir() && !level.getBlockState(check).canBeReplaced()) {
                return;
            }
        }

        for (int y = 0; y < height; y++) {
            level.setBlock(origin.above(y), log, 3);
        }
        int crownBase = height - 2;
        for (int dy = 0; dy <= 3; dy++) {
            int radius = dy == 3 ? 1 : (dy == 0 ? 1 : 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && (dy == 2 || dy == 3)) continue;
                    BlockPos p = origin.offset(dx, crownBase + dy, dz);
                    if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) {
                        level.setBlock(p, leaves, 3);
                    }
                }
            }
        }
    }
}
