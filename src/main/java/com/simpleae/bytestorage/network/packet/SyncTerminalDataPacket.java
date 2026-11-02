package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.TerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Пакет синхронізації віртуального сховища (S2C - Server to Client).
 * Відправляє клієнту зведений список предметів та статистику зайнятих/вільних байтів.
 */
public class SyncTerminalDataPacket {

    private final int totalCapacity;
    private final int totalUsed;
    private final List<ItemStack> items;

    public SyncTerminalDataPacket(int totalCapacity, int totalUsed, List<ItemStack> items) {
        this.totalCapacity = totalCapacity;
        this.totalUsed = totalUsed;
        this.items = items;
    }

    public SyncTerminalDataPacket(FriendlyByteBuf buf) {
        this.totalCapacity = buf.readVarInt();
        this.totalUsed = buf.readVarInt();
        int size = buf.readVarInt();
        this.items = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                ItemStack stack = ItemStack.of(tag);
                if (tag.contains("ByteCount")) {
                    stack.setCount(tag.getInt("ByteCount"));
                }
                this.items.add(stack);
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(totalCapacity);
        buf.writeVarInt(totalUsed);
        buf.writeVarInt(items.size());

        for (ItemStack stack : items) {
            CompoundTag tag = stack.save(new CompoundTag());
            tag.putInt("ByteCount", stack.getCount());
            buf.writeNbt(tag);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().player != null &&
                Minecraft.getInstance().player.containerMenu instanceof TerminalMenu menu) {
                menu.updateClientData(totalCapacity, totalUsed, items);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
