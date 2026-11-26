package com.simpleae.bytestorage.dimension.block.entity;

import com.simpleae.bytestorage.block.entity.AssemblerBlockEntity;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ускоренные асемблеры измерения Silicon Plane:
 * - Advanced Assembler: x2 скорость, минимум 2 тика на крафт.
 * - Singular Assembler: x4 скорость, минимум 1 тик на крафт.
 */
public class TieredAssemblerBlockEntity extends AssemblerBlockEntity {

    private final boolean singular;

    public TieredAssemblerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean singular) {
        super(type, pos, state);
        this.singular = singular;
    }

    public static TieredAssemblerBlockEntity createAdvanced(BlockPos pos, BlockState state) {
        return new TieredAssemblerBlockEntity(SimpleAeDimension.ADVANCED_ASSEMBLER_ENTITY.get(), pos, state, false);
    }

    public static TieredAssemblerBlockEntity createSingular(BlockPos pos, BlockState state) {
        return new TieredAssemblerBlockEntity(SimpleAeDimension.SINGULAR_ASSEMBLER_ENTITY.get(), pos, state, true);
    }

    @Override
    protected int getSpeedMultiplier() {
        return singular ? 4 : 2;
    }

    @Override
    protected int getMinCraftTicks() {
        return singular ? 1 : 2;
    }

    @Override
    public int getIdleEnergyDrain() {
        return singular ? 12 : 6;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(singular
                ? "block.simple_bytestorage_dimension.singular_assembler"
                : "block.simple_bytestorage_dimension.advanced_assembler");
    }
}
