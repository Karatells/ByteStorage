package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.menu.ControllerMenu;
import com.simpleae.bytestorage.util.CraftingService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Управление заказом из экрана Контроллера (C2S): приоритет, пауза, отмена. */
public class ControllerQueuePacket {

    public static final int PRIORITY_UP = 0;
    public static final int PRIORITY_DOWN = 1;
    public static final int TOGGLE_PAUSE = 2;
    public static final int CANCEL = 3;

    private final int action;
    private final String patternId;

    public ControllerQueuePacket(int action, String patternId) {
        this.action = action;
        this.patternId = patternId;
    }

    public ControllerQueuePacket(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.patternId = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeUtf(patternId, 256);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !(player.containerMenu instanceof ControllerMenu menu)) {
                return;
            }
            NetworkControllerBlockEntity controller = menu.getController();
            if (controller == null || !menu.stillValid(player)) {
                return;
            }
            switch (action) {
                case PRIORITY_UP -> controller.changeQueuePriority(patternId, 1);
                case PRIORITY_DOWN -> controller.changeQueuePriority(patternId, -1);
                case TOGGLE_PAUSE -> controller.toggleQueuePaused(patternId);
                case CANCEL -> {
                    if (controller.getGrid() != null) {
                        CraftingService.cancel(controller.getGrid(), patternId);
                    }
                }
                default -> {
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
