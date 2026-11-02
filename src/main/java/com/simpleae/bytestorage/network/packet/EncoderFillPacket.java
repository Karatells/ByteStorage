package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.PatternEncodingTerminalMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Заповнити Термінал Кодування рецептом (C2S): надсилає інтеграція з JEI («+» біля рецепта).
 */
public class EncoderFillPacket {

    private final boolean processing;
    private final List<ItemStack> inputs;
    private final ItemStack output;

    public EncoderFillPacket(boolean processing, List<ItemStack> inputs, ItemStack output) {
        this.processing = processing;
        this.inputs = inputs;
        this.output = output;
    }

    public EncoderFillPacket(FriendlyByteBuf buf) {
        this.processing = buf.readBoolean();
        int size = buf.readVarInt();
        this.inputs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buf.readItem();
            stack.setCount(Math.max(1, buf.readVarInt()));
            this.inputs.add(stack);
        }
        this.output = buf.readItem();
        if (!this.output.isEmpty()) {
            this.output.setCount(Math.max(1, buf.readVarInt()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(processing);
        buf.writeVarInt(inputs.size());
        for (ItemStack stack : inputs) {
            ItemStack one = stack.copy();
            one.setCount(1);
            buf.writeItem(one);
            buf.writeVarInt(Math.max(1, stack.getCount()));
        }
        ItemStack out = output.copy();
        out.setCount(1);
        buf.writeItem(out);
        if (!output.isEmpty()) {
            buf.writeVarInt(Math.max(1, output.getCount()));
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof PatternEncodingTerminalMenu menu
                    && menu.getEncoder() != null) {
                menu.getEncoder().applyRecipe(processing, inputs, output);
            }
        });
        ctx.setPacketHandled(true);
    }
}
