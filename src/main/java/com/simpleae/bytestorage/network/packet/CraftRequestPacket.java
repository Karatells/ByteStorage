package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.menu.IGridMenu;
import com.simpleae.bytestorage.util.CraftingService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Автокрафт предмета из любого терминала (C2S): рассчитать план или заказать. */
public class CraftRequestPacket {

    public static final int ACTION_PLAN = 0;
    public static final int ACTION_ORDER = 1;
    public static final int MAX_AMOUNT = 100_000;

    private final int action;
    private final ItemStack stack;
    private final int amount;

    public CraftRequestPacket(int action, ItemStack stack, int amount) {
        this.action = action;
        this.stack = stack;
        this.amount = amount;
    }

    public CraftRequestPacket(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.stack = buf.readItem();
        this.amount = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        ItemStack one = stack.copy();
        one.setCount(1);
        buf.writeItem(one);
        buf.writeVarInt(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !(player.containerMenu instanceof IGridMenu menu) || stack.isEmpty()) {
                return;
            }
            int items = Math.max(1, Math.min(MAX_AMOUNT, amount));
            if (action == ACTION_ORDER) {
                player.displayClientMessage(CraftingService.orderOutput(menu.getMenuGrid(), stack, items), true);
                return;
            }
            IGrid grid = menu.getMenuGrid();
            if (grid == null || !grid.getState().isActive()) {
                player.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_network"), true);
                return;
            }
            PatternData found = CraftingService.findPatternForOutput(grid, stack);
            if (found == null) {
                player.displayClientMessage(Component.translatable("msg.simple_bytestorage.order.no_pattern"), true);
                return;
            }
            int perCraft = Math.max(1, found.getOutput().getCount());
            int crafts = Math.max(1, Math.min(CraftingService.MAX_CRAFTS, (items + perCraft - 1) / perCraft));
            CraftingService.Plan plan = CraftingService.plan(grid, found, crafts);
            int maxAmount = plan.missing.isEmpty() ? 0 : CraftingService.maxPossibleCrafts(grid, found, crafts) * perCraft;
            SimpleByteStoragePackets.sendToPlayer(CraftPlanPacket.of(found.getId(), crafts, plan, maxAmount), player);
        });
        ctx.setPacketHandled(true);
    }
}
