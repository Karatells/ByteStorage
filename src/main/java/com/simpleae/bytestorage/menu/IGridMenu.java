package com.simpleae.bytestorage.menu;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.network.packet.CraftableListPacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.util.CraftingService;
import net.minecraft.server.level.ServerPlayer;

/** Меню, що працює з мережею й дозволяє замовляти автокрафт (звичайний термінал, термінал крафту). */
public interface IGridMenu {

    IGrid getMenuGrid();

    /** Надіслати клієнту перелік предметів, які мережа вміє крафтити. */
    default void sendCraftables(ServerPlayer player) {
        IGrid grid = getMenuGrid();
        if (grid != null && grid.getState().isActive()) {
            SimpleByteStoragePackets.sendToPlayer(new CraftableListPacket(CraftingService.craftableOutputs(grid)), player);
        }
    }
}
