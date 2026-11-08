package com.simpleae.bytestorage.event;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.item.WirelessConfig;
import com.simpleae.bytestorage.item.WirelessTerminalItem;
import com.simpleae.bytestorage.item.WirelessUpgradeItem;
import com.simpleae.bytestorage.api.WirelessMacros;
import com.simpleae.bytestorage.util.InsuranceData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import java.util.ArrayList;
import java.util.List;
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
        return findTerminal(player, WirelessUpgradeItem.Type.AUTO_TRANSFER, ENERGY_PER_ITEM);
    }

    /** Терминал игрока с установленным улучшением нужного вида, привязанный и с достаточным зарядом. */
    private static ItemStack findTerminal(ServerPlayer player, WirelessUpgradeItem.Type type, int minEnergy) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isUsable(stack, type, minEnergy)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isUsable(ItemStack stack, WirelessUpgradeItem.Type type, int minEnergy) {
        return stack.getItem() instanceof WirelessTerminalItem
                && WirelessTerminalItem.isBound(stack)
                && WirelessTerminalItem.getEnergy(stack) >= minEnergy
                && WirelessConfig.hasUpgrade(stack, type);
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

    // --- Карта магнита ---

    private static final int MAGNET_INTERVAL = 4;
    private static final double MAGNET_RADIUS = 6.0;
    private static final int MAGNET_MAX_ITEMS = 16;

    /** Притягивает выпавшие предметы к игроку (не при приседании); 1 FE за предмет за такт, фильтр терминала учитывается. */
    @SubscribeEvent
    public static void onMagnetTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % MAGNET_INTERVAL != 0 || player.isShiftKeyDown() || player.isSpectator()) {
            return;
        }
        ItemStack terminal = findTerminal(player, WirelessUpgradeItem.Type.MAGNET, 1);
        if (terminal.isEmpty()) {
            return;
        }
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(MAGNET_RADIUS),
                e -> e.isAlive() && e.tickCount >= 20 && !e.getPersistentData().contains("PreventRemoteMovement")
                        && WirelessConfig.allows(terminal, e.getItem()));
        if (items.isEmpty()) {
            return;
        }
        int energy = WirelessTerminalItem.getEnergy(terminal);
        net.minecraft.world.phys.Vec3 target = player.position().add(0, player.getBbHeight() / 2.0, 0);
        int moved = 0;
        for (ItemEntity item : items) {
            if (moved >= MAGNET_MAX_ITEMS || energy <= 0) {
                break;
            }
            double distance = item.position().distanceTo(target);
            if (distance < 0.8) {
                continue;
            }
            net.minecraft.world.phys.Vec3 pull = target.subtract(item.position()).normalize()
                    .scale(Math.min(0.4, 0.1 + distance * 0.05));
            item.setDeltaMotion(pull);
            item.hasImpulse = true;
            moved++;
            energy--;
        }
        if (moved > 0) {
            WirelessTerminalItem.setEnergy(terminal, energy);
        }
    }

    // --- Карта инструментов ---

    public static final int TOOL_COST = 20;
    public static final int MACRO_COST = 50;

    @SubscribeEvent
    public static void onToolBroken(PlayerDestroyItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() == null) return;
        ItemStack original = event.getOriginal();
        if (original.isEmpty()) return;
        ItemStack terminal = findTerminal(player, WirelessUpgradeItem.Type.TOOL, TOOL_COST);
        if (terminal.isEmpty()) return;
        INetworkStorage storage = storageOf(terminal, player);
        if (storage == null) return;
        ItemStack replacement = storage.extractItem(original.getItem(), 1, false);
        if (replacement.isEmpty()) return;
        WirelessTerminalItem.setEnergy(terminal, WirelessTerminalItem.getEnergy(terminal) - TOOL_COST);
        player.setItemInHand(event.getHand(), replacement);
        player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.tool_replaced",
                replacement.getHoverName()), true);
    }

    // --- Карта страховки ---

    @SubscribeEvent
    public static void onDeathDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getDrops().isEmpty()) return;
        ItemStack terminal = ItemStack.EMPTY;
        for (var drop : event.getDrops()) {
            if (isUsable(drop.getItem(), WirelessUpgradeItem.Type.INSURANCE, ENERGY_PER_ITEM)) {
                terminal = drop.getItem();
                break;
            }
        }
        if (terminal.isEmpty()) return;
        NetworkControllerBlockEntity controller = WirelessTerminalItem.resolveController(terminal, player.server);
        if (controller == null || controller.getGrid() == null || !controller.getGrid().getState().isActive()) return;
        INetworkStorage storage = controller.getGrid().getStorage();
        if (storage == null) return;

        InsuranceData data = InsuranceData.get(player.server);
        InsuranceData.Record record = data.open(player.getUUID(), controller.getLevel().dimension().location().toString(),
                controller.getBlockPos());
        int energy = WirelessTerminalItem.getEnergy(terminal);
        int stored = 0;
        // Сам терминал обрабатываем последним, чтобы потраченная энергия учитывалась на нём
        List<net.minecraft.world.entity.item.ItemEntity> order = new ArrayList<>(event.getDrops());
        final ItemStack terminalRef = terminal;
        order.sort((a, b) -> Boolean.compare(a.getItem() == terminalRef, b.getItem() == terminalRef));
        for (var drop : order) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) continue;
            int limit = Math.min(stack.getCount(), energy / ENERGY_PER_ITEM);
            if (limit <= 0) break;
            ItemStack offer = stack.copy();
            offer.setCount(limit);
            ItemStack rest = storage.insertItem(offer, false);
            int moved = limit - rest.getCount();
            if (moved <= 0) continue;
            energy -= moved * ENERGY_PER_ITEM;
            stored += moved;
            ItemStack recorded = stack.copy();
            recorded.setCount(moved);
            record.items.add(recorded);
            stack.shrink(moved);
            if (stack.isEmpty()) {
                drop.discard();
                event.getDrops().remove(drop);
            }
        }
        if (stored > 0) {
            if (!terminal.isEmpty()) {
                WirelessTerminalItem.setEnergy(terminal, energy);
            }
            data.touch();
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.insured", stored), false);
        } else if (record.items.isEmpty()) {
            data.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) return;
        InsuranceData data = InsuranceData.get(player.server);
        InsuranceData.Record record = data.get(player.getUUID());
        if (record == null) return;

        net.minecraft.server.level.ServerLevel level = player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(record.dimension)));
        if (level == null || !level.isLoaded(record.controller)
                || !(level.getBlockEntity(record.controller) instanceof NetworkControllerBlockEntity controller)
                || controller.getGrid() == null || !controller.getGrid().getState().isActive()) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.insurance_unreachable"), false);
            return;
        }
        INetworkStorage storage = controller.getGrid().getStorage();
        int restored = 0;
        for (ItemStack wanted : record.items) {
            ItemStack template = wanted.copy();
            template.setCount(1);
            ItemStack got = storage.extractItem(template, wanted.getCount(), false);
            if (got.isEmpty()) continue;
            restored += got.getCount();
            give(player, got);
        }
        data.remove(player.getUUID());
        player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.restored", restored), false);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);
        if (slot.getType() == EquipmentSlot.Type.ARMOR && stack.getCount() == 1 && player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, stack);
            return;
        }
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    // --- Карта макроса ---

    /** Запуск макроса по горячей клавише: обработчики аддонов выполняют действие для названия, заданного в терминале. */
    public static void runMacro(ServerPlayer player) {
        ItemStack terminal = findTerminal(player, WirelessUpgradeItem.Type.MACRO, MACRO_COST);
        if (terminal.isEmpty()) {
            return;
        }
        String macro = WirelessConfig.macroName(terminal);
        if (macro.isEmpty()) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.macro_unset"), true);
            return;
        }
        NetworkControllerBlockEntity controller = WirelessTerminalItem.resolveController(terminal, player.server);
        if (controller == null || controller.getGrid() == null || !controller.getGrid().getState().isActive()) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.no_network"), true);
            return;
        }
        int done = 0;
        for (WirelessMacros.Handler handler : WirelessMacros.handlers()) {
            done += handler.run(player, controller.getGrid(), macro);
        }
        if (done > 0) {
            WirelessTerminalItem.setEnergy(terminal, WirelessTerminalItem.getEnergy(terminal) - MACRO_COST);
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.macro_ok", macro, done), true);
        } else {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.macro_none", macro), true);
        }
    }
}
