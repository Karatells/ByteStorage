package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.ControllerMenu;
import com.simpleae.bytestorage.util.NetworkStats;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Статистика мережі для екрана Контролера (S2C). */
public class ControllerStatsPacket {

    private final NetworkStats stats;

    public ControllerStatsPacket(NetworkStats stats) {
        this.stats = stats;
    }

    public ControllerStatsPacket(FriendlyByteBuf buf) {
        this.stats = NetworkStats.read(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        stats.write(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.containerMenu instanceof ControllerMenu menu) {
                menu.setStats(stats);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
