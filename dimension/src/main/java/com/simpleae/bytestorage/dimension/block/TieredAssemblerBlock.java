package com.simpleae.bytestorage.dimension.block;

import com.simpleae.bytestorage.block.AssemblerBlock;
import com.simpleae.bytestorage.block.entity.AssemblerBlockEntity;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.block.entity.TieredAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ускоренный Асемблер (Advanced / Singular), совместимый с автокрафтом и GUI базового мода.
 */
public class TieredAssemblerBlock extends AssemblerBlock {

    private final boolean singular;

    public TieredAssemblerBlock(Properties properties, boolean singular) {
        super(properties);
        this.singular = singular;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return singular
                ? TieredAssemblerBlockEntity.createSingular(pos, state)
                : TieredAssemblerBlockEntity.createAdvanced(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        BlockEntityType<TieredAssemblerBlockEntity> expected = singular
                ? SimpleAeDimension.SINGULAR_ASSEMBLER_ENTITY.get()
                : SimpleAeDimension.ADVANCED_ASSEMBLER_ENTITY.get();
        return createTickerHelper(blockEntityType, expected, AssemblerBlockEntity::serverTick);
    }
}
