package com.simpleae.bytestorage.dimension.block.entity;

import com.simpleae.bytestorage.block.entity.PatternProviderBlockEntity;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Шина шаблонов Mk2: работает в 2.5 раза чаще (каждые 4 тика) и подаёт до 64 рецептов за цикл.
 */
public class PatternBusMk2BlockEntity extends PatternProviderBlockEntity {

    public PatternBusMk2BlockEntity(BlockPos pos, BlockState state) {
        super(SimpleAeDimension.PATTERN_BUS_MK2_ENTITY.get(), pos, state);
    }

    @Override
    protected int getCycleTicks() {
        return 4;
    }

    @Override
    protected int getPushesPerCycle() {
        return 64;
    }

    @Override
    public int getIdleEnergyDrain() {
        return 5;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage_dimension.pattern_bus_mk2");
    }
}
