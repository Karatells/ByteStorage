package com.simpleae.bytestorage.event;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.network.packet.UserPrefsPacket;
import com.simpleae.bytestorage.util.UserPrefsStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Отправляет игроку его избранное и сохранённые поиски при входе. */
@Mod.EventBusSubscriber(modid = SimpleByteStorage.MOD_ID)
public final class UserPrefsEvents {

    private UserPrefsEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SimpleByteStoragePackets.sendToPlayer(new UserPrefsPacket(UserPrefsStore.load(player)), player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer from && event.getEntity() instanceof ServerPlayer to) {
            UserPrefsStore.copy(from, to);
        }
    }
}
