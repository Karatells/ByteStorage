package com.simpleae.bytestorage.scripting.net;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.scripting.ScriptingConfig;
import com.simpleae.bytestorage.scripting.block.entity.LogicBusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Серверна логіка інтерфейсу Логічної шини: призначення ідентифікатора та список ID мережі. */
final class BusService {

    private BusService() {
    }

    static void handle(ServerPlayer player, BlockPos pos, int action, String text) {
        if (!player.level().isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 144.0) {
            return;
        }
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof LogicBusBlockEntity bus)) {
            return;
        }
        boolean canEdit = ScriptingConfig.canEdit(player);
        if (action == BusActionPacket.SET_ID && canEdit) {
            bus.setIdentifier(text);
        }

        IGrid grid = bus.getGrid();
        boolean online = grid != null && grid.getState().isActive();
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (grid != null) {
            for (IGridNode node : grid.getNodes()) {
                if (node instanceof LogicBusBlockEntity other && !other.isRemoved()
                        && other.getLevel() == bus.getLevel() && !other.getIdentifier().isEmpty()) {
                    counts.merge(other.getIdentifier(), 1, Integer::sum);
                }
            }
        } else if (!bus.getIdentifier().isEmpty()) {
            counts.put(bus.getIdentifier(), 1);
        }
        List<BusStatePacket.IdCount> ids = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            ids.add(new BusStatePacket.IdCount(entry.getKey(), entry.getValue()));
        }
        BusStatePacket reply = new BusStatePacket(ScriptingConfig.ENABLED.get(), online, canEdit,
                bus.getIdentifier(), bus.getStatus(), bus.isEnabled(), ids);
        ScriptingPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), reply);
    }
}
