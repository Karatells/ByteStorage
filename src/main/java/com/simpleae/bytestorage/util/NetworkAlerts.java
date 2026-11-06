package com.simpleae.bytestorage.util;

import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.item.WirelessTerminalItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Предупреждения сети: сообщение в чат игрокам, у которых в инвентаре есть Беспроводной терминал этой сети. */
public final class NetworkAlerts {

    private NetworkAlerts() {
    }

    /** @return сколько игроков получили сообщение */
    public static int notifyOwners(NetworkControllerBlockEntity controller, Component message) {
        if (controller.getLevel() == null || controller.getLevel().isClientSide) {
            return 0;
        }
        MinecraftServer server = controller.getLevel().getServer();
        if (server == null) {
            return 0;
        }
        Component full = Component.translatable("msg.simple_bytestorage.alert",
                controller.getBlockPos().getX(), controller.getBlockPos().getY(), controller.getBlockPos().getZ(), message)
                .withStyle(net.minecraft.ChatFormatting.GOLD);
        int sent = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean owns = false;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof WirelessTerminalItem && WirelessTerminalItem.isBound(stack)
                        && WirelessTerminalItem.resolveController(stack, server) == controller) {
                    owns = true;
                    break;
                }
            }
            if (owns) {
                player.sendSystemMessage(full);
                sent++;
            }
        }
        return sent;
    }
}
