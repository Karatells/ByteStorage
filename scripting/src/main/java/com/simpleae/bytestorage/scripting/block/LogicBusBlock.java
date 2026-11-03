package com.simpleae.bytestorage.scripting.block;

import com.simpleae.bytestorage.scripting.SimpleAeScripting;
import com.simpleae.bytestorage.scripting.block.entity.LogicBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Логічна шина: виконує скрипт. Налаштовується через Логічний термінал. */
public class LogicBusBlock extends BaseEntityBlock {

    public LogicBusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogicBusBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, SimpleAeScripting.LOGIC_BUS_ENTITY.get(), LogicBusBlockEntity::serverTick);
    }
}
