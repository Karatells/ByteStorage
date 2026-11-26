package com.simpleae.bytestorage.dimension.portal;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.block.RiftPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Поиск и валидация прямоугольной рамки Врат Разлома (от 4×5 до 12×12 блоков)
 * из Кремниевого базальта (silicon_basalt) с Ядром Врат Разлома (rift_gate_core) в основании.
 */
public final class RiftFrameHelper {

    public static final int MIN_INNER_WIDTH = 2;
    public static final int MAX_INNER_WIDTH = 10;
    public static final int MIN_INNER_HEIGHT = 3;
    public static final int MAX_INNER_HEIGHT = 10;

    public record Frame(BlockPos bottomLeftInner, Direction.Axis axis, int innerWidth, int innerHeight) {

        public void fillWithPortal(Level level) {
            Direction horiz = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            BlockState portalState = SimpleAeDimension.RIFT_PORTAL_BLOCK.get()
                    .defaultBlockState()
                    .setValue(RiftPortalBlock.AXIS, axis);
            for (int x = 0; x < innerWidth; x++) {
                for (int y = 0; y < innerHeight; y++) {
                    BlockPos p = bottomLeftInner.relative(horiz, x).above(y);
                    level.setBlock(p, portalState, 3);
                }
            }
        }
    }

    public static boolean isFrameBlock(BlockState state) {
        return state.is(SimpleAeDimension.SILICON_BASALT_BLOCK.get())
                || state.is(SimpleAeDimension.RIFT_GATE_CORE_BLOCK.get());
    }

    public static boolean isInteriorReplaceable(BlockState state) {
        return state.isAir() || state.is(SimpleAeDimension.RIFT_PORTAL_BLOCK.get());
    }

    /**
     * Пытается найти валидную замкнутую рамку, в нижнем ряду которой стоит corePos.
     */
    @Nullable
    public static Frame findValidFrame(Level level, BlockPos corePos) {
        Frame xFrame = tryAxis(level, corePos, Direction.Axis.X);
        if (xFrame != null) return xFrame;
        return tryAxis(level, corePos, Direction.Axis.Z);
    }

    @Nullable
    private static Frame tryAxis(Level level, BlockPos corePos, Direction.Axis axis) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction left = right.getOpposite();

        // Поднимаемся на 1 блок выше ядра — это должна быть внутренняя область портала
        BlockPos startInner = corePos.above();
        if (!isInteriorReplaceable(level.getBlockState(startInner))) {
            return null;
        }

        // Идём влево до левой стенки рамки
        BlockPos leftMost = startInner;
        for (int i = 0; i < MAX_INNER_WIDTH; i++) {
            BlockPos next = leftMost.relative(left);
            if (isFrameBlock(level.getBlockState(next))) {
                break;
            }
            if (!isInteriorReplaceable(level.getBlockState(next))) {
                return null;
            }
            leftMost = next;
        }
        if (!isFrameBlock(level.getBlockState(leftMost.relative(left)))) {
            return null;
        }

        // Измеряем внутреннюю ширину вправо
        int width = 0;
        while (width <= MAX_INNER_WIDTH) {
            BlockPos cur = leftMost.relative(right, width);
            if (isFrameBlock(level.getBlockState(cur))) {
                break;
            }
            if (!isInteriorReplaceable(level.getBlockState(cur))) {
                return null;
            }
            width++;
        }
        if (width < MIN_INNER_WIDTH || width > MAX_INNER_WIDTH) {
            return null;
        }
        if (!isFrameBlock(level.getBlockState(leftMost.relative(right, width)))) {
            return null;
        }

        // Измеряем внутреннюю высоту вверх
        int height = 0;
        while (height <= MAX_INNER_HEIGHT) {
            BlockPos cur = leftMost.above(height);
            if (isFrameBlock(level.getBlockState(cur))) {
                break;
            }
            if (!isInteriorReplaceable(level.getBlockState(cur))) {
                return null;
            }
            height++;
        }
        if (height < MIN_INNER_HEIGHT || height > MAX_INNER_HEIGHT) {
            return null;
        }

        // Проверяем весь периметр рамки и всю внутреннюю область
        for (int x = 0; x < width; x++) {
            // Низ и верх рамки
            if (!isFrameBlock(level.getBlockState(leftMost.relative(right, x).below()))) return null;
            if (!isFrameBlock(level.getBlockState(leftMost.relative(right, x).above(height)))) return null;
            // Внутренность
            for (int y = 0; y < height; y++) {
                if (!isInteriorReplaceable(level.getBlockState(leftMost.relative(right, x).above(y)))) {
                    return null;
                }
            }
        }
        for (int y = 0; y < height; y++) {
            // Левая и правая стойки
            if (!isFrameBlock(level.getBlockState(leftMost.relative(left).above(y)))) return null;
            if (!isFrameBlock(level.getBlockState(leftMost.relative(right, width).above(y)))) return null;
        }

        return new Frame(leftMost, axis, width, height);
    }

    private RiftFrameHelper() {
    }
}
