package com.simpleae.bytestorage.util;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Запись событий в журнал контроллера сети (безопасно вызывать с любой нодой; на клиенте ничего не делает). */
public final class NetworkJournal {

    private NetworkJournal() {
    }

    public static void log(IGrid grid, int type, ItemStack stack, int amount) {
        log(grid, type, stack, amount, "");
    }

    public static void log(IGrid grid, int type, ItemStack stack, int amount, String text) {
        if (grid == null || stack.isEmpty() || amount <= 0) {
            return;
        }
        NetworkControllerBlockEntity controller = grid.getController();
        Level level = controller == null ? null : controller.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) {
            controller.getJournal().add(level.getGameTime(), type, id.toString(), amount, text);
        }
    }
}
