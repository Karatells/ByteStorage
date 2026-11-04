package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.menu.PatternTerminalMenu;
import com.simpleae.bytestorage.util.RecipeLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Рецепты Библиотеки рецептов для Терминала шаблонов (S2C) в компактном виде: id, результат и маска нехватки.
 * Раскладку по сетке клиент восстанавливает сам из синхронизированных рецептов игры.
 */
public class SyncLibraryPacket {

    private final List<String> ids;
    private final List<ItemStack> outputs;
    private final List<Integer> masks;

    public SyncLibraryPacket(List<PatternData> patterns, List<Integer> masks) {
        this.ids = new ArrayList<>(patterns.size());
        this.outputs = new ArrayList<>(patterns.size());
        for (PatternData p : patterns) {
            ids.add(p.getId());
            outputs.add(p.getOutput());
        }
        this.masks = masks;
    }

    public SyncLibraryPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        this.ids = new ArrayList<>(n);
        this.outputs = new ArrayList<>(n);
        this.masks = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ids.add(buf.readUtf(128));
            outputs.add(buf.readItem());
            masks.add(buf.readVarInt());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            buf.writeUtf(ids.get(i), 128);
            buf.writeItem(outputs.get(i));
            buf.writeVarInt(Math.max(0, masks.get(i)));
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || !(mc.player.containerMenu instanceof PatternTerminalMenu menu)) {
                return;
            }
            List<PatternData> patterns = new ArrayList<>(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                List<ItemStack> grid = new ArrayList<>(9);
                var recipe = mc.level.getRecipeManager().byKey(new ResourceLocation(id.substring(RecipeLibrary.PREFIX.length())));
                if (recipe.isPresent() && recipe.get() instanceof CraftingRecipe crafting) {
                    List<Ingredient> cells = RecipeLibrary.layout(crafting);
                    if (cells != null) {
                        for (Ingredient ingredient : cells) {
                            ItemStack[] options = ingredient.getItems();
                            grid.add(ingredient.isEmpty() || options.length == 0 ? ItemStack.EMPTY : options[0].copy());
                        }
                    }
                }
                patterns.add(new PatternData(id, outputs.get(i), grid));
            }
            menu.updateClientLibrary(patterns, masks);
        }));
        ctx.setPacketHandled(true);
    }
}
