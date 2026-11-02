package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.network.packet.SyncPatternDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Реєстрація та відправка мережевих пакетів SimpleAE.
 */
public class SimpleByteStoragePackets {

    private static final String PROTOCOL_VERSION = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SimpleByteStorage.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.messageBuilder(SyncTerminalDataPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncTerminalDataPacket::new)
                .encoder(SyncTerminalDataPacket::encode)
                .consumerMainThread(SyncTerminalDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(TerminalActionPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TerminalActionPacket::new)
                .encoder(TerminalActionPacket::encode)
                .consumerMainThread(TerminalActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncCraftingTerminalDataPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncCraftingTerminalDataPacket::new)
                .encoder(SyncCraftingTerminalDataPacket::encode)
                .consumerMainThread(SyncCraftingTerminalDataPacket::handle)
                .add();

        // --- Пакети системи автокрафту (Термінал Шаблонів) ---
        CHANNEL.messageBuilder(SyncPatternDataPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncPatternDataPacket::new)
                .encoder(SyncPatternDataPacket::encode)
                .consumerMainThread(SyncPatternDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(CraftPlanPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CraftPlanPacket::new)
                .encoder(CraftPlanPacket::encode)
                .consumerMainThread(CraftPlanPacket::handle)
                .add();

        CHANNEL.messageBuilder(PatternOrderPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PatternOrderPacket::new)
                .encoder(PatternOrderPacket::encode)
                .consumerMainThread(PatternOrderPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }
}
