package com.simpleae.bytestorage.api;

import com.simpleae.bytestorage.api.network.IGrid;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * API для аддонів: обробники макросів Беспроводного терміналу (карта «Макрос»).
 * Аддон реєструє обробник; ядро викликає його, коли гравець натискає клавішу макроса.
 */
public final class WirelessMacros {

    @FunctionalInterface
    public interface Handler {
        /**
         * @param macro назва макроса, задана гравцем в налаштуваннях терміналу
         * @return кількість виконаних дій (0 — нічого не зроблено)
         */
        int run(ServerPlayer player, IGrid grid, String macro);
    }

    private static final List<Handler> HANDLERS = new ArrayList<>();

    private WirelessMacros() {
    }

    public static synchronized void register(Handler handler) {
        HANDLERS.add(handler);
    }

    public static synchronized List<Handler> handlers() {
        return new ArrayList<>(HANDLERS);
    }
}
