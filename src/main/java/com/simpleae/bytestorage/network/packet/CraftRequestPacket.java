package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.IGridMenu;
import com.simpleae.bytestorage.util.CraftingService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Замовлення автокрафту предмета з будь-якого терміналу (C2S). */
public class CraftRequestPacket {

    public static final int MAX_AMOUNT = 100_000;

    private final ItemStack stack;
    private final int amount;

    public CraftRequestPacket(ItemStack stack, int amount) {
        this.stack = stack;
        this.amount = amount;
    }

    public CraftRequestPacket(FriendlyByteBuf buf) {
        this.stack = buf.readItem();
        this.amount = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        ItemStack one = stack.copy();
        one.setCount(1);
        buf.writeItem(one);
        buf.writeVarInt(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof IGridMenu menu && !stack.isEmpty()) {
                player.displayClientMessage(CraftingService.orderOutput(menu.getMenuGrid(), stack,
                        Math.max(1, Math.min(MAX_AMOUNT, amount))), true);
            }
        });
        ctx.setPacketHandled(true);
    }
}
