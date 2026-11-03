package com.simpleae.bytestorage.scripting.net;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.scripting.ScriptingConfig;
import com.simpleae.bytestorage.scripting.block.entity.LogicBusBlockEntity;
import com.simpleae.bytestorage.scripting.block.entity.LogicTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Серверна логіка Логічного терміналу: перевірки доступу, застосування дій, відповідь станом. */
final class TerminalService {

    private TerminalService() {
    }

    static void handle(ServerPlayer player, BlockPos terminalPos, int action, BlockPos busPos, int intArg, String text) {
        if (!player.level().isLoaded(terminalPos)
                || player.distanceToSqr(Vec3.atCenterOf(terminalPos)) > 144.0) {
            return;
        }
        BlockEntity be = player.level().getBlockEntity(terminalPos);
        if (!(be instanceof LogicTerminalBlockEntity terminal)) {
            return;
        }
        IGrid grid = terminal.getGrid();
        boolean online = grid != null && grid.getState().isActive();

        List<LogicBusBlockEntity> buses = new ArrayList<>();
        if (online) {
            for (IGridNode node : grid.getNodes()) {
                if (node instanceof LogicBusBlockEntity bus && !bus.isRemoved()
                        && bus.getLevel() == terminal.getLevel()) {
                    buses.add(bus);
                }
            }
            buses.sort(Comparator.comparingLong((LogicBusBlockEntity b) -> b.getBlockPos().asLong()));
        }
        LogicBusBlockEntity selected = null;
        for (LogicBusBlockEntity bus : buses) {
            if (bus.getBlockPos().equals(busPos)) {
                selected = bus;
                break;
            }
        }

        boolean canEdit = ScriptingConfig.canEdit(player);
        if (selected != null && canEdit) {
            switch (action) {
                case ScriptActionPacket.SAVE -> selected.setScript(text);
                case ScriptActionPacket.ENABLE -> selected.setEnabled(intArg != 0);
                case ScriptActionPacket.INTERVAL -> selected.setInterval(intArg);
                default -> {
                }
            }
        }

        ScriptStatePacket.BusInfo[] infos = new ScriptStatePacket.BusInfo[buses.size()];
        for (int i = 0; i < infos.length; i++) {
            LogicBusBlockEntity bus = buses.get(i);
            infos[i] = new ScriptStatePacket.BusInfo(bus.getBlockPos(), bus.isEnabled(), bus.getInterval(), bus.getStatus(), bus.getIdentifier());
        }
        boolean withScript = action == ScriptActionPacket.LIST && selected != null;
        ScriptStatePacket reply = new ScriptStatePacket(
                ScriptingConfig.ENABLED.get(), online, canEdit, ScriptingConfig.MIN_INTERVAL.get(),
                ScriptingConfig.MAX_SCRIPT_LENGTH.get(),
                List.of(infos),
                selected == null ? null : selected.getBlockPos(),
                withScript,
                withScript ? selected.getScript() : "",
                selected == null ? List.of() : selected.getLog());
        ScriptingPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), reply);
    }
}
