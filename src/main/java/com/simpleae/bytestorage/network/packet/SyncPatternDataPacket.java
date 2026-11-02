package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.menu.PatternTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
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

    public SyncPatternDataPacket(int usedBytes, int capacityBytes, int assemblerCount,
                                 List<PatternData> patterns, List<Boolean> craftable) {
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
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.containerMenu instanceof PatternTerminalMenu menu) {
                menu.updateClientData(usedBytes, capacityBytes, assemblerCount, patterns, craftable);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
