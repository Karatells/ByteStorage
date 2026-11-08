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
 * Реєстрація та відправка мережевих пакетів Simple Byte Storage.
 */
public class SimpleByteStoragePackets {

    private static final String PROTOCOL_VERSION = "11";
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
        CHANNEL.messageBuilder(SyncLibraryPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncLibraryPacket::new)
                .encoder(SyncLibraryPacket::encode)
                .consumerMainThread(SyncLibraryPacket::handle)
                .add();

        CHANNEL.messageBuilder(WirelessSettingsPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(WirelessSettingsPacket::new)
                .encoder(WirelessSettingsPacket::encode)
                .consumerMainThread(WirelessSettingsPacket::handle)
                .add();
        CHANNEL.messageBuilder(WirelessMacroPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(WirelessMacroPacket::new)
                .encoder(WirelessMacroPacket::encode)
                .consumerMainThread(WirelessMacroPacket::handle)
                .add();

        CHANNEL.messageBuilder(CraftableListPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CraftableListPacket::new)
                .encoder(CraftableListPacket::encode)
                .consumerMainThread(CraftableListPacket::handle)
                .add();
        CHANNEL.messageBuilder(CraftRequestPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CraftRequestPacket::new)
                .encoder(CraftRequestPacket::encode)
                .consumerMainThread(CraftRequestPacket::handle)
                .add();

        CHANNEL.messageBuilder(ControllerQueuePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ControllerQueuePacket::new)
                .encoder(ControllerQueuePacket::encode)
                .consumerMainThread(ControllerQueuePacket::handle)
                .add();

        CHANNEL.messageBuilder(ControllerStatsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ControllerStatsPacket::new)
                .encoder(ControllerStatsPacket::encode)
                .consumerMainThread(ControllerStatsPacket::handle)
                .add();

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

        CHANNEL.messageBuilder(EncoderFillPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(EncoderFillPacket::new)
                .encoder(EncoderFillPacket::encode)
                .consumerMainThread(EncoderFillPacket::handle)
                .add();

        CHANNEL.messageBuilder(PatternOrderPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PatternOrderPacket::new)
                .encoder(PatternOrderPacket::encode)
                .consumerMainThread(PatternOrderPacket::handle)
                .add();

        CHANNEL.messageBuilder(TerminalAvailabilityPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(TerminalAvailabilityPacket::new)
                .encoder(TerminalAvailabilityPacket::encode)
                .consumerMainThread(TerminalAvailabilityPacket::handle)
                .add();

        CHANNEL.messageBuilder(UserPrefsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(UserPrefsPacket::new)
                .encoder(UserPrefsPacket::encode)
                .consumerMainThread(UserPrefsPacket::handle)
                .add();
        CHANNEL.messageBuilder(UserPrefsActionPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UserPrefsActionPacket::new)
                .encoder(UserPrefsActionPacket::encode)
                .consumerMainThread(UserPrefsActionPacket::handle)
                .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        CHANNEL.sendToServer(message);
    }
}
