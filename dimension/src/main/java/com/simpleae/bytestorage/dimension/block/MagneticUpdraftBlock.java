package com.simpleae.bytestorage.dimension.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Восходящий магнитный поток (Magnetic Updraft) — природный энергетический лифт,
 * поднимающий игрока с поверхности к парящим островам Облачного Архива.
 */
public class MagneticUpdraftBlock extends Block {

    public MagneticUpdraftBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        Vec3 vel = entity.getDeltaMovement();
        if (vel.y < 0.45) {
            entity.setDeltaMovement(vel.x * 0.92, 0.45, vel.z * 0.92);
        }
        entity.resetFallDistance();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0, 0.14, 0.0);
        }
    }
}
