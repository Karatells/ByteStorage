package com.simpleae.bytestorage.scripting.net;

import com.simpleae.bytestorage.scripting.SimpleAeScripting;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Мережевий канал аддона. */
public final class ScriptingPackets {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SimpleAeScripting.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ScriptingPackets() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ScriptActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ScriptActionPacket::new)
                .encoder(ScriptActionPacket::encode)
                .consumerMainThread(ScriptActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(ScriptStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ScriptStatePacket::new)
                .encoder(ScriptStatePacket::encode)
                .consumerMainThread(ScriptStatePacket::handle)
                .add();
    }
}
