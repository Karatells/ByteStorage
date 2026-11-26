package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Резная оптическая книжная полка (6 слотов под книги с визуальным отображением заполненности).
 */
public class ChiseledOpticBookshelfBlock extends ChiseledBookShelfBlock {

    public ChiseledOpticBookshelfBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChiseledOpticBookshelfBlockEntity(pos, state);
    }

    public static class ChiseledOpticBookshelfBlockEntity extends ChiseledBookShelfBlockEntity {
        public ChiseledOpticBookshelfBlockEntity(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public BlockEntityType<?> getType() {
            return SimpleAeDimension.CHISELED_OPTIC_BOOKSHELF_ENTITY.get();
        }
    }
}
