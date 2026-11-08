package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.client.gui.TerminalExtras;
import com.simpleae.bytestorage.util.UserPrefs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** S2C: избранное и сохранённые поиски игрока (полное состояние). */
public class UserPrefsPacket {

    private final List<String> favorites;
    private final List<String> searches;

    public UserPrefsPacket(UserPrefs prefs) {
        this.favorites = prefs.favoriteList();
        this.searches = new ArrayList<>(prefs.searches());
    }

    public UserPrefsPacket(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), UserPrefs.MAX_FAVORITES);
        this.favorites = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            favorites.add(buf.readUtf(UserPrefs.MAX_KEY_LENGTH));
        }
        this.searches = new ArrayList<>();
        for (int i = 0; i < UserPrefs.SEARCH_SLOTS; i++) {
            searches.add(buf.readUtf(UserPrefs.MAX_SEARCH_LENGTH));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(favorites.size());
        for (String key : favorites) {
            buf.writeUtf(key, UserPrefs.MAX_KEY_LENGTH);
        }
        for (int i = 0; i < UserPrefs.SEARCH_SLOTS; i++) {
            buf.writeUtf(i < searches.size() ? searches.get(i) : "", UserPrefs.MAX_SEARCH_LENGTH);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                TerminalExtras.applyServerPrefs(favorites, searches)));
        ctx.setPacketHandled(true);
    }
}
