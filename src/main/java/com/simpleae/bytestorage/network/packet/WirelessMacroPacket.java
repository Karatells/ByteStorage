package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.event.WirelessAutoTransfer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Запуск макроса Беспроводного терминала по горячей клавише (C2S, без данных). */
public class WirelessMacroPacket {

    public WirelessMacroPacket() {
    }

    public WirelessMacroPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                WirelessAutoTransfer.runMacro(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
