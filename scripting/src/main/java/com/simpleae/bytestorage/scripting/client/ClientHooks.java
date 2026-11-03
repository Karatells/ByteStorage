package com.simpleae.bytestorage.scripting.client;

import com.simpleae.bytestorage.scripting.net.ScriptStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Клієнтські точки входу (викликаються через DistExecutor, щоб серверу не потрібні були класи GUI). */
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openTerminal(BlockPos pos) {
        Minecraft.getInstance().setScreen(new LogicTerminalScreen(pos));
    }

    public static void openBus(BlockPos pos) {
        Minecraft.getInstance().setScreen(new LogicBusScreen(pos));
    }

    public static void onBusState(com.simpleae.bytestorage.scripting.net.BusStatePacket packet) {
        if (Minecraft.getInstance().screen instanceof LogicBusScreen screen) {
            screen.apply(packet);
        }
    }

    public static void onState(ScriptStatePacket packet) {
        if (Minecraft.getInstance().screen instanceof LogicTerminalScreen screen) {
            screen.apply(packet);
        }
    }
}
