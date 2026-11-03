package com.simpleae.bytestorage.scripting.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Дія гравця в інтерфейсі Логічної шини (C2S). */
public class BusActionPacket {

    /** Запросити актуальний стан. */
    public static final int REFRESH = 0;
    /** text = новий ідентифікатор цієї шини (порожній — прибрати). */
    public static final int SET_ID = 1;

    private final BlockPos bus;
    private final int action;
    private final String text;

    public BusActionPacket(BlockPos bus, int action, String text) {
        this.bus = bus;
        this.action = action;
        this.text = text == null ? "" : text;
    }

    public BusActionPacket(FriendlyByteBuf buf) {
        this.bus = buf.readBlockPos();
        this.action = buf.readVarInt();
        this.text = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(bus);
        buf.writeVarInt(action);
        buf.writeUtf(text, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                BusService.handle(player, bus, action, text);
            }
        });
        ctx.setPacketHandled(true);
    }
}
