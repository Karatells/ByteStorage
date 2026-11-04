package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.WirelessSettingsMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Название макроса из экрана настроек Беспроводного терминала (C2S). */
public class WirelessSettingsPacket {

    private final String macro;

    public WirelessSettingsPacket(String macro) {
        this.macro = macro;
    }

    public WirelessSettingsPacket(FriendlyByteBuf buf) {
        this.macro = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(macro, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof WirelessSettingsMenu menu) {
                menu.setMacro(macro);
            }
        });
        ctx.setPacketHandled(true);
    }
}
