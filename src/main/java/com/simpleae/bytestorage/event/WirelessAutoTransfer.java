package com.simpleae.bytestorage.event;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.item.WirelessConfig;
import com.simpleae.bytestorage.item.WirelessTerminalItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Карта автообмена Беспроводного терминала: пополняет предметы в руках из сети и
 * забирает подобранные предметы в сеть (с учётом фильтра). Расходует энергию терминала.
 */
@Mod.EventBusSubscriber(modid = SimpleByteStorage.MOD_ID)
public final class WirelessAutoTransfer {

    public static final int ENERGY_PER_ITEM = 2;
    private static final int RESTOCK_INTERVAL = 5;

    private WirelessAutoTransfer() {
    }

    /** Терминал игрока с активной картой автообмена, привязанный и с доступной сетью; иначе пусто. */
    private static ItemStack findTerminal(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof WirelessTerminalItem
                    && WirelessTerminalItem.isBound(stack)
                    && WirelessTerminalItem.getEnergy(stack) >= ENERGY_PER_ITEM
                    && WirelessConfig.hasAutoTransfer(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static INetworkStorage storageOf(ItemStack terminal, ServerPlayer player) {
        NetworkControllerBlockEntity controller = WirelessTerminalItem.resolveController(terminal, player.server);
        if (controller == null) return null;
        IGrid grid = controller.getGrid();
        if (grid == null || !grid.getState().isActive()) return null;
        return grid.getStorage();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % RESTOCK_INTERVAL != 0) return;

        ItemStack terminal = findTerminal(player);
        if (terminal.isEmpty() || !WirelessConfig.restock(terminal)) return;
        INetworkStorage storage = storageOf(terminal, player);
        if (storage == null) return;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty() || held.getItem() instanceof WirelessTerminalItem
                    || !held.isStackable() || held.getCount() >= held.getMaxStackSize()
                    || !WirelessConfig.allows(terminal, held)) {
                continue;
            }
            int energy = WirelessTerminalItem.getEnergy(terminal);
            int want = Math.min(held.getMaxStackSize() - held.getCount(), energy / ENERGY_PER_ITEM);
            if (want <= 0) return;
            ItemStack template = held.copy();
            template.setCount(1);
            ItemStack got = storage.extractItem(template, want, false);
            if (!got.isEmpty()) {
                held.grow(got.getCount());
                WirelessTerminalItem.setEnergy(terminal, energy - got.getCount() * ENERGY_PER_ITEM);
            }
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemEntity entity = event.getItem();
        ItemStack picked = entity.getItem();
        if (picked.isEmpty()) return;

        ItemStack terminal = findTerminal(player);
        if (terminal.isEmpty() || !WirelessConfig.pickup(terminal) || !WirelessConfig.allows(terminal, picked)) return;
        INetworkStorage storage = storageOf(terminal, player);
        if (storage == null) return;

        int energy = WirelessTerminalItem.getEnergy(terminal);
        int limit = Math.min(picked.getCount(), energy / ENERGY_PER_ITEM);
        if (limit <= 0) return;
        ItemStack offer = picked.copy();
        offer.setCount(limit);
        ItemStack remainder = storage.insertItem(offer, false);
        int moved = limit - remainder.getCount();
        if (moved <= 0) return;

        WirelessTerminalItem.setEnergy(terminal, energy - moved * ENERGY_PER_ITEM);
        player.take(entity, moved);
        picked.shrink(moved);
        if (picked.isEmpty()) {
            entity.discard();
        }
        event.setCanceled(true);
    }
}
