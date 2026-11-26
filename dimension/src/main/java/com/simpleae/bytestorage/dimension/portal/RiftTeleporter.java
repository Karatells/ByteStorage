package com.simpleae.bytestorage.dimension.portal;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.block.RiftPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Безопасная телепортация между Обычным миром и Кремниевым Планом:
 * ищет существующие Врата Разлома в радиусе 64 блоков, либо строит ответную платформу и портал.
 */
public class RiftTeleporter implements ITeleporter {

    private final BlockPos originPos;

    public RiftTeleporter(BlockPos originPos) {
        this.originPos = originPos;
    }

    @Override
    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld,
                              float yaw, Function<Boolean, Entity> repositionEntity) {
        Entity moved = repositionEntity.apply(false);
        if (moved != null) {
            moved.setPortalCooldown();
        }
        return moved;
    }

    @Nullable
    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                    Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        BlockPos portalBase = findExistingPortal(destWorld, originPos, 64);
        if (portalBase == null) {
            portalBase = buildReturnPortal(destWorld, originPos);
        }
        // Высаживаем игрока на платформу ПЕРЕД порталом (+2 по Z), а не внутрь блока портала!
        BlockPos safeStand = portalBase.offset(0, 0, 2);
        return new PortalInfo(
                new Vec3(safeStand.getX() + 0.5, safeStand.getY() + 0.1, safeStand.getZ() + 0.5),
                Vec3.ZERO,
                180.0F,
                0.0F
        );
    }

    @Nullable
    private BlockPos findExistingPortal(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int minY = Math.max(level.getMinBuildHeight() + 2, 20);
        int maxY = Math.min(level.getMaxBuildHeight() - 4, 220);

        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (!level.isLoaded(new BlockPos(x, 64, z))) continue;
                for (int y = minY; y <= maxY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p).is(SimpleAeDimension.RIFT_PORTAL_BLOCK.get())) {
                        // Спускаемся к низу портала
                        BlockPos base = p;
                        while (level.getBlockState(base.below()).is(SimpleAeDimension.RIFT_PORTAL_BLOCK.get())) {
                            base = base.below();
                        }
                        double d = base.distSqr(center);
                        if (d < bestDist) {
                            bestDist = d;
                            best = base;
                        }
                        break;
                    }
                }
            }
        }
        return best;
    }

    private BlockPos buildReturnPortal(ServerLevel level, BlockPos near) {
        int x = near.getX();
        int z = near.getZ();
        // Загружаем чанк, чтобы Heightmap вернул корректную высоту
        level.getChunk(x >> 4, z >> 4);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (surfaceY < 52 || surfaceY > 180) {
            surfaceY = 74;
        }
        BlockPos base = new BlockPos(x, surfaceY, z);

        BlockState basalt = SimpleAeDimension.SILICON_BASALT_BLOCK.get().defaultBlockState();
        BlockState core = SimpleAeDimension.RIFT_GATE_CORE_BLOCK.get().defaultBlockState();
        BlockState portal = SimpleAeDimension.RIFT_PORTAL_BLOCK.get().defaultBlockState()
                .setValue(RiftPortalBlock.AXIS, Direction.Axis.X);

        // Платформа 5x5 под порталом и расчистка воздуха 5x5x5 над ней
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(base.offset(dx, -1, dz), basalt, 3);
                for (int dy = 0; dy <= 4; dy++) {
                    level.setBlock(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Рамка 4x5 (внутри 2x3) по оси X
        for (int dx = -1; dx <= 2; dx++) {
            level.setBlock(base.offset(dx, 0, 0), dx == 0 ? core : basalt, 3);
            level.setBlock(base.offset(dx, 4, 0), basalt, 3);
        }
        for (int dy = 1; dy <= 3; dy++) {
            level.setBlock(base.offset(-1, dy, 0), basalt, 3);
            level.setBlock(base.offset(2, dy, 0), basalt, 3);
            level.setBlock(base.offset(0, dy, 0), portal, 3);
            level.setBlock(base.offset(1, dy, 0), portal, 3);
        }

        return base.offset(0, 1, 0);
    }
}
