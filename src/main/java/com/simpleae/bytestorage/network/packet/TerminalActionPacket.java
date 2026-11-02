package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.CraftingTerminalMenu;
import com.simpleae.bytestorage.menu.TerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Пакет дій гравця в терміналі (C2S - Client to Server).
 * Відправляє запити на вилучення або вставку предметів у віртуальне сховище.
 */
public class TerminalActionPacket {

    public static final int ACTION_EXTRACT_STACK = 0;
    public static final int ACTION_EXTRACT_HALF = 1;
    public static final int ACTION_EXTRACT_SHIFT = 2;
    public static final int ACTION_INSERT_CURSOR = 3;
    public static final int ACTION_INSERT_ONE = 4;
    public static final int ACTION_DEPOSIT_ALL = 5;

    private final ItemStack targetItem;
    private final int actionType;

    public TerminalActionPacket(ItemStack targetItem, int actionType) {
        this.targetItem = targetItem;
        this.actionType = actionType;
    }

    public TerminalActionPacket(FriendlyByteBuf buf) {
        this.targetItem = buf.readItem();
        this.actionType = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(targetItem != null ? targetItem : ItemStack.EMPTY);
        buf.writeVarInt(actionType);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                if (player.containerMenu instanceof TerminalMenu menu) {
                    menu.handleClientAction(targetItem, actionType, player);
                } else if (player.containerMenu instanceof CraftingTerminalMenu craftingMenu) {
                    craftingMenu.handleClientAction(targetItem, actionType, player);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
