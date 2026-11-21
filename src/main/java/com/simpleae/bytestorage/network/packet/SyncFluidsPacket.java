package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.TerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Синхронизация списка жидкостей сети (из FluidCellItem) на клиент для вкладки Fluids в Терминале. */
public class SyncFluidsPacket {

    private final List<FluidStack> fluids;

    public SyncFluidsPacket(List<FluidStack> fluids) {
        this.fluids = fluids;
    }

    public SyncFluidsPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.fluids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            FluidStack fs = FluidStack.readFromPacket(buf);
            if (!fs.isEmpty()) {
                this.fluids.add(fs);
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(fluids.size());
        for (FluidStack fs : fluids) {
            fs.writeToPacket(buf);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.containerMenu instanceof TerminalMenu menu) {
                menu.updateClientFluids(this.fluids);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
