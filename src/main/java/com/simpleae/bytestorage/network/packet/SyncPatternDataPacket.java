package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.menu.PatternTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Синхронізація глобального списку шаблонів (S2C) для Терміналу Шаблонів:
 * усі шаблони мережі, прапорець «ресурсів достатньо» для кожного, статистика байтової пам'яті
 * та кількість Асемблерів у мережі.
 */
public class SyncPatternDataPacket {

    private final int usedBytes;
    private final int capacityBytes;
    private final int assemblerCount;
    private final List<PatternData> patterns;
    private final List<Boolean> craftable;
    private final List<CraftPlanPacket.Entry> queue;
    private final List<String> queueIds;

    public SyncPatternDataPacket(int usedBytes, int capacityBytes, int assemblerCount,
                                 List<PatternData> patterns, List<Boolean> craftable,
                                 List<String> queueIds, List<CraftPlanPacket.Entry> queue) {
        this.queueIds = queueIds;
        this.queue = queue;
        this.usedBytes = Math.max(0, usedBytes);
        this.capacityBytes = Math.max(0, capacityBytes);
        this.assemblerCount = Math.max(0, assemblerCount);
        this.patterns = patterns;
        this.craftable = craftable;
    }

    public SyncPatternDataPacket(FriendlyByteBuf buf) {
        this.usedBytes = buf.readVarInt();
        this.capacityBytes = buf.readVarInt();
        this.assemblerCount = buf.readVarInt();
        int size = buf.readVarInt();
        this.patterns = new ArrayList<>(size);
        this.craftable = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.patterns.add(PatternData.read(buf));
            this.craftable.add(buf.readBoolean());
        }
        int queueSize = buf.readVarInt();
        this.queueIds = new ArrayList<>(queueSize);
        this.queue = new ArrayList<>(queueSize);
        for (int i = 0; i < queueSize; i++) {
            this.queueIds.add(buf.readUtf(128));
            ItemStack stack = buf.readItem();
            int crafts = buf.readVarInt();
            boolean active = buf.readBoolean();
            this.queue.add(new CraftPlanPacket.Entry(stack, crafts, active));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(usedBytes);
        buf.writeVarInt(capacityBytes);
        buf.writeVarInt(assemblerCount);
        buf.writeVarInt(patterns.size());
        for (int i = 0; i < patterns.size(); i++) {
            patterns.get(i).write(buf);
            buf.writeBoolean(craftable.get(i));
        }
        buf.writeVarInt(queue.size());
        for (int i = 0; i < queue.size(); i++) {
            CraftPlanPacket.Entry entry = queue.get(i);
            buf.writeUtf(queueIds.get(i), 128);
            ItemStack one = entry.stack.copy();
            one.setCount(1);
            buf.writeItem(one);
            buf.writeVarInt(entry.count);
            buf.writeBoolean(entry.processing);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.containerMenu instanceof PatternTerminalMenu menu) {
                menu.updateClientData(usedBytes, capacityBytes, assemblerCount, patterns, craftable);
                menu.updateClientQueue(queueIds, queue);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
