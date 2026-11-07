package com.simpleae.bytestorage.client;

import com.simpleae.bytestorage.util.CraftingService;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/** Клиентская копия резервов/производства для подсказок терминалов. */
public final class TerminalAvailabilityClient {

    private static Map<String, Integer> reserved = Map.of();
    private static Map<String, Integer> crafting = Map.of();

    private TerminalAvailabilityClient() {
    }

    public static void update(Map<String, Integer> newReserved, Map<String, Integer> newCrafting) {
        reserved = newReserved;
        crafting = newCrafting;
    }

    public static int reserved(ItemStack stack) {
        return reserved.getOrDefault(CraftingService.key(stack), 0);
    }

    public static int crafting(ItemStack stack) {
        return crafting.getOrDefault(CraftingService.key(stack), 0);
    }
}
