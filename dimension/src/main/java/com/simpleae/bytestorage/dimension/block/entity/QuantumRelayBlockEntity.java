package com.simpleae.bytestorage.dimension.block.entity;

import com.simpleae.bytestorage.block.entity.CraftingCoprocessorBlockEntity;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Квантовое реле: сетевой узел без энергопотребления, добавляющий +2 к мощности сопроцессоров крафта.
 */
public class QuantumRelayBlockEntity extends CraftingCoprocessorBlockEntity {

    public QuantumRelayBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleAeDimension.QUANTUM_RELAY_ENTITY.get(), pos, state);
    }

    @Override
    public int getCoprocessorPower() {
        return 2;
    }

    @Override
    public int getIdleEnergyDrain() {
        return 0;
    }
}
