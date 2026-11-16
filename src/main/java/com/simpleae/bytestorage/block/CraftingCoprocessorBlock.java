package com.simpleae.bytestorage.block;

import com.simpleae.bytestorage.block.entity.CraftingCoprocessorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Сопроцесор крафту: прискорює роботу Асемблерів і Шин шаблонів у мережі. */
public class CraftingCoprocessorBlock extends BaseEntityBlock {

    public CraftingCoprocessorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingCoprocessorBlockEntity(pos, state);
    }
}
