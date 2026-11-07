package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.client.TerminalAvailabilityClient;
import com.simpleae.bytestorage.util.TerminalAvailability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** S2C: резервы и производство для подсказок терминала. */
public class TerminalAvailabilityPacket {

    private final Map<String, Integer> reserved;
    private final Map<String, Integer> crafting;

    public TerminalAvailabilityPacket(TerminalAvailability data) {
        this.reserved = data.reserved;
        this.crafting = data.crafting;
    }

    public TerminalAvailabilityPacket(FriendlyByteBuf buf) {
        this.reserved = read(buf);
        this.crafting = read(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        write(buf, reserved);
        write(buf, crafting);
    }

    private static Map<String, Integer> read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            map.put(buf.readUtf(512), buf.readVarInt());
        }
        return map;
    }

    private static void write(FriendlyByteBuf buf, Map<String, Integer> map) {
        buf.writeVarInt(map.size());
        map.forEach((k, v) -> {
            buf.writeUtf(k, 512);
            buf.writeVarInt(v);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                TerminalAvailabilityClient.update(reserved, crafting)));
        ctx.setPacketHandled(true);
    }
}
