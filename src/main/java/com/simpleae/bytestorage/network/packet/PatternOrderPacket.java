package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.PatternTerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Дія гравця в Терміналі Шаблонів (C2S): замовити крафт шаблону або видалити шаблон з мережі.
 */
public class PatternOrderPacket {

    public static final int ACTION_ORDER = 0;
    public static final int ACTION_REMOVE = 1;
    /** Попросити сервер порахувати план замовлення (для вікна підтвердження). */
    public static final int ACTION_PLAN = 2;
    /** Скасувати всі замовлення цього шаблону в усіх Асемблерах та Шинах. */
    public static final int ACTION_CANCEL = 3;

    private final int action;
    private final String patternId;
    private final int amount;

    public PatternOrderPacket(int action, String patternId, int amount) {
        this.action = action;
        this.patternId = patternId == null ? "" : patternId;
        this.amount = amount;
    }

    public PatternOrderPacket(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.patternId = buf.readUtf(128);
        this.amount = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeUtf(patternId, 128);
        buf.writeVarInt(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof PatternTerminalMenu menu) {
                menu.handleAction(player, action, patternId, amount);
            }
        });
        ctx.setPacketHandled(true);
    }
}
