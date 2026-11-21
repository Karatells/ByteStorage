package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.TerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** Клик по вкладке Fluids в Терминале: налить жидкость в пустое ведро или слить полное ведро в сеть. */
public class FluidTerminalActionPacket {

    @Nullable
    private final ResourceLocation fluidId;

    public FluidTerminalActionPacket(@Nullable ResourceLocation fluidId) {
        this.fluidId = fluidId;
    }

    public FluidTerminalActionPacket(FriendlyByteBuf buf) {
        this.fluidId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(fluidId != null);
        if (fluidId != null) {
            buf.writeResourceLocation(fluidId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof TerminalMenu menu) {
                menu.handleFluidAction(this.fluidId, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
