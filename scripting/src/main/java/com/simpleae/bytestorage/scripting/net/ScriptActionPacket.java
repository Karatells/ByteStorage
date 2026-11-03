package com.simpleae.bytestorage.scripting.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Дія гравця в Логічному терміналі (C2S). */
public class ScriptActionPacket {

    /** Список шин + (за вибраної шини) скрипт. */
    public static final int LIST = 0;
    /** Лише оновити статус/лог, без скрипта. */
    public static final int REFRESH = 1;
    public static final int SAVE = 2;
    /** intArg: 1 = запустити, 0 = зупинити. */
    public static final int ENABLE = 3;
    public static final int INTERVAL = 4;
    /** text = новий ідентифікатор шини (порожній — прибрати). */
    public static final int SET_ID = 5;

    private final BlockPos terminal;
    private final int action;
    private final BlockPos bus;
    private final int intArg;
    private final String text;

    public ScriptActionPacket(BlockPos terminal, int action, BlockPos bus, int intArg, String text) {
        this.terminal = terminal;
        this.action = action;
        this.bus = bus == null ? BlockPos.ZERO : bus;
        this.intArg = intArg;
        this.text = text == null ? "" : text;
    }

    public ScriptActionPacket(FriendlyByteBuf buf) {
        this.terminal = buf.readBlockPos();
        this.action = buf.readVarInt();
        this.bus = buf.readBlockPos();
        this.intArg = buf.readVarInt();
        this.text = buf.readUtf(32767);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(terminal);
        buf.writeVarInt(action);
        buf.writeBlockPos(bus);
        buf.writeVarInt(intArg);
        buf.writeUtf(text, 32767);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                TerminalService.handle(player, terminal, action, bus, intArg, text);
            }
        });
        ctx.setPacketHandled(true);
    }
}
