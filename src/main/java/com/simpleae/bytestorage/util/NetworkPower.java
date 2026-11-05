package com.simpleae.bytestorage.util;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

/** Передача энергии сети (FE) в соседние машины: используют Шина Шаблонов и Энергоинтерфейс. */
public final class NetworkPower {

    /** Максимум FE/тик на одно устройство. */
    public static final int MAX_PER_TICK = 1000;

    private NetworkPower() {
    }

    /**
     * Заряжает соседние блоки с энергией (кроме узлов сети) из буфера контроллера.
     *
     * @param ticks за сколько тиков накоплен бюджет (устройство вызывает метод раз в несколько тиков)
     * @return сколько FE передано
     */
    public static int feed(IGrid grid, Level level, BlockPos pos, int ticks) {
        if (grid == null || level == null || level.isClientSide || !grid.getState().isActive()) {
            return 0;
        }
        NetworkControllerBlockEntity controller = grid.getController();
        if (controller == null) {
            return 0;
        }
        int budget = MAX_PER_TICK * Math.max(1, ticks);
        int total = 0;
        for (Direction direction : Direction.values()) {
            if (budget <= 0) {
                break;
            }
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour == null || neighbour instanceof IGridNode) {
                continue;
            }
            IEnergyStorage storage = neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
            if (storage == null || !storage.canReceive()) {
                continue;
            }
            int want = storage.receiveEnergy(budget, true);
            if (want <= 0) {
                continue;
            }
            int drawn = controller.drawSurplusEnergy(want);
            if (drawn <= 0) {
                break;
            }
            int accepted = storage.receiveEnergy(drawn, false);
            if (accepted < drawn) {
                controller.refundEnergy(drawn - accepted);
            }
            budget -= accepted;
            total += accepted;
        }
        return total;
    }
}
