package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.CraftingTerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Заповнити крафтову сітку 3×3 Термінала Крафту з JEI (C2S).
 * Для кожного з 9 слотів передається список допустимих варіантів (наприклад, різні дошки для тегу).
 */
public class CraftingTerminalJeiFillPacket {

    private static final int MAX_SLOTS = 9;
    private static final int MAX_CANDIDATES_PER_SLOT = 32;

    private final List<List<ItemStack>> slotCandidates;
    private final boolean maxTransfer;

    public CraftingTerminalJeiFillPacket(List<List<ItemStack>> slotCandidates, boolean maxTransfer) {
        this.slotCandidates = slotCandidates;
        this.maxTransfer = maxTransfer;
    }

    public CraftingTerminalJeiFillPacket(FriendlyByteBuf buf) {
        this.maxTransfer = buf.readBoolean();
        int slotCount = Math.min(MAX_SLOTS, buf.readVarInt());
        this.slotCandidates = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            int candCount = Math.min(MAX_CANDIDATES_PER_SLOT, buf.readVarInt());
            List<ItemStack> cands = new ArrayList<>(candCount);
            for (int j = 0; j < candCount; j++) {
                cands.add(buf.readItem());
            }
            this.slotCandidates.add(cands);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(maxTransfer);
        int slotCount = Math.min(MAX_SLOTS, slotCandidates.size());
        buf.writeVarInt(slotCount);
        for (int i = 0; i < slotCount; i++) {
            List<ItemStack> cands = slotCandidates.get(i);
            int candCount = Math.min(MAX_CANDIDATES_PER_SLOT, cands.size());
            buf.writeVarInt(candCount);
            for (int j = 0; j < candCount; j++) {
                ItemStack one = cands.get(j).copy();
                if (!one.isEmpty()) one.setCount(1);
                buf.writeItem(one);
            }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof CraftingTerminalMenu menu) {
                menu.fillFromJei(slotCandidates, maxTransfer, player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
