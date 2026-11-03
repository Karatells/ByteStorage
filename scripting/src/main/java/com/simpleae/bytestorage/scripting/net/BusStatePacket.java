package com.simpleae.bytestorage.scripting.net;

import com.simpleae.bytestorage.scripting.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Стан інтерфейсу Логічної шини для клієнта (S2C). */
public class BusStatePacket {

    /** Ідентифікатор і скільки шин мережі його мають. */
    public record IdCount(String id, int count) {
    }

    public final boolean modEnabled;
    public final boolean online;
    public final boolean canEdit;
    public final String ownId;
    public final String status;
    public final boolean running;
    public final List<IdCount> ids;

    public BusStatePacket(boolean modEnabled, boolean online, boolean canEdit, String ownId, String status,
                          boolean running, List<IdCount> ids) {
        this.modEnabled = modEnabled;
        this.online = online;
        this.canEdit = canEdit;
        this.ownId = ownId;
        this.status = status;
        this.running = running;
        this.ids = ids;
    }

    public BusStatePacket(FriendlyByteBuf buf) {
        this.modEnabled = buf.readBoolean();
        this.online = buf.readBoolean();
        this.canEdit = buf.readBoolean();
        this.ownId = buf.readUtf(64);
        this.status = buf.readUtf(300);
        this.running = buf.readBoolean();
        int count = buf.readVarInt();
        this.ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(new IdCount(buf.readUtf(64), buf.readVarInt()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(modEnabled);
        buf.writeBoolean(online);
        buf.writeBoolean(canEdit);
        buf.writeUtf(ownId, 64);
        buf.writeUtf(status, 300);
        buf.writeBoolean(running);
        buf.writeVarInt(ids.size());
        for (IdCount entry : ids) {
            buf.writeUtf(entry.id(), 64);
            buf.writeVarInt(entry.count());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onBusState(this)));
        ctx.setPacketHandled(true);
    }
}
