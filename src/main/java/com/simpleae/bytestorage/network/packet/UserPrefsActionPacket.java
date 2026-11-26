package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.util.UserPrefs;
import com.simpleae.bytestorage.util.UserPrefsStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** C2S: изменение личных настроек (избранное, сохранённый поиск) или перенос старых локальных данных. */
public class UserPrefsActionPacket {

    public static final int SET_FAVORITE = 0;
    public static final int SET_SEARCH = 1;
    public static final int MERGE = 2;

    private final int action;
    private final String text;
    private final int number;
    private final List<String> list;

    private UserPrefsActionPacket(int action, String text, int number, List<String> list) {
        this.action = action;
        this.text = text;
        this.number = number;
        this.list = list;
    }

    public static UserPrefsActionPacket setFavorite(String key, boolean on) {
        return new UserPrefsActionPacket(SET_FAVORITE, key, on ? 1 : 0, List.of());
    }

    public static UserPrefsActionPacket setSearch(int slot, String text) {
        return new UserPrefsActionPacket(SET_SEARCH, text, slot, List.of());
    }

    /** @param list избранные ключи, затем {@link UserPrefs#SEARCH_SLOTS} поисков */
    public static UserPrefsActionPacket merge(List<String> favorites, List<String> searches) {
        List<String> all = new ArrayList<>(favorites);
        all.addAll(searches);
        return new UserPrefsActionPacket(MERGE, "", favorites.size(), all);
    }

    public UserPrefsActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.text = buf.readUtf(UserPrefs.MAX_KEY_LENGTH);
        this.number = buf.readVarInt();
        int n = Math.min(buf.readVarInt(), UserPrefs.MAX_FAVORITES + UserPrefs.SEARCH_SLOTS);
        this.list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(buf.readUtf(UserPrefs.MAX_KEY_LENGTH));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeUtf(text, UserPrefs.MAX_KEY_LENGTH);
        buf.writeVarInt(number);
        buf.writeVarInt(list.size());
        for (String s : list) {
            buf.writeUtf(s, UserPrefs.MAX_KEY_LENGTH);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            UserPrefs prefs = UserPrefsStore.load(player);
            switch (action) {
                case SET_FAVORITE -> prefs.setFavorite(text, number != 0);
                case SET_SEARCH -> prefs.setSearch(number, text);
                case MERGE -> {
                    int favCount = Math.max(0, Math.min(number, list.size()));
                    prefs.merge(list.subList(0, favCount), list.subList(favCount, list.size()));
                }
                default -> {
                    return;
                }
            }
            UserPrefsStore.save(player, prefs);
            SimpleByteStoragePackets.sendToPlayer(new UserPrefsPacket(prefs), player);
        });
        ctx.setPacketHandled(true);
    }
}
