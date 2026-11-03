package com.simpleae.bytestorage.scripting.net;

import com.simpleae.bytestorage.scripting.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Стан Логічного терміналу для клієнта (S2C). */
public class ScriptStatePacket {

    /** Короткий опис шини в списку. */
    public record BusInfo(BlockPos pos, boolean enabled, int interval, String status) {
    }

    public final boolean modEnabled;
    public final boolean online;
    public final boolean canEdit;
    public final int minInterval;
    public final int maxLength;
    public final List<BusInfo> buses;
    @Nullable
    public final BlockPos selected;
    public final boolean withScript;
    public final String script;
    public final List<String> log;

    public ScriptStatePacket(boolean modEnabled, boolean online, boolean canEdit, int minInterval, int maxLength,
                             List<BusInfo> buses, @Nullable BlockPos selected, boolean withScript,
                             String script, List<String> log) {
        this.modEnabled = modEnabled;
        this.online = online;
        this.canEdit = canEdit;
        this.minInterval = minInterval;
        this.maxLength = maxLength;
        this.buses = buses;
        this.selected = selected;
        this.withScript = withScript;
        this.script = script;
        this.log = log;
    }

    public ScriptStatePacket(FriendlyByteBuf buf) {
        this.modEnabled = buf.readBoolean();
        this.online = buf.readBoolean();
        this.canEdit = buf.readBoolean();
        this.minInterval = buf.readVarInt();
        this.maxLength = buf.readVarInt();
        int count = buf.readVarInt();
        this.buses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            buses.add(new BusInfo(buf.readBlockPos(), buf.readBoolean(), buf.readVarInt(), buf.readUtf(300)));
        }
        this.selected = buf.readBoolean() ? buf.readBlockPos() : null;
        this.withScript = buf.readBoolean();
        this.script = buf.readUtf(32767);
        int logCount = buf.readVarInt();
        this.log = new ArrayList<>();
        for (int i = 0; i < logCount; i++) {
            log.add(buf.readUtf(300));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(modEnabled);
        buf.writeBoolean(online);
        buf.writeBoolean(canEdit);
        buf.writeVarInt(minInterval);
        buf.writeVarInt(maxLength);
        buf.writeVarInt(buses.size());
        for (BusInfo info : buses) {
            buf.writeBlockPos(info.pos());
            buf.writeBoolean(info.enabled());
            buf.writeVarInt(info.interval());
            buf.writeUtf(info.status(), 300);
        }
        buf.writeBoolean(selected != null);
        if (selected != null) {
            buf.writeBlockPos(selected);
        }
        buf.writeBoolean(withScript);
        buf.writeUtf(script, 32767);
        buf.writeVarInt(log.size());
        for (String line : log) {
            buf.writeUtf(line, 300);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onState(this)));
        ctx.setPacketHandled(true);
    }
}
