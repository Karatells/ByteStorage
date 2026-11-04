package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.client.gui.TerminalExtras;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Список предметів, які можна замовити в автокрафті (S2C), для звичайних терміналів. */
public class CraftableListPacket {

    private final List<ItemStack> items;

    public CraftableListPacket(List<ItemStack> items) {
        this.items = items;
    }

    public CraftableListPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        this.items = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            this.items.add(buf.readItem());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(items.size());
        for (ItemStack stack : items) {
            ItemStack one = stack.copy();
            one.setCount(1);
            buf.writeItem(one);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TerminalExtras.setCraftables(items)));
        ctx.setPacketHandled(true);
    }
}
