package com.simpleae.bytestorage.compat.jei;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.client.gui.CraftingTerminalScreen;
import com.simpleae.bytestorage.client.gui.TerminalScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Плагін JEI: «+» біля рецептів переносить їх у Термінал Крафту та Термінал Кодування Шаблонів. */
@JeiPlugin
public class SimpleAeJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(SimpleByteStorage.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(CraftingTerminalScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(CraftingTerminalScreen screen) {
                return List.of(new Rect2i(screen.getGuiLeft() + screen.getXSize(), screen.getGuiTop(), 48, 100));
            }
        });
        registration.addGuiContainerHandler(TerminalScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(TerminalScreen screen) {
                return List.of(new Rect2i(screen.getGuiLeft() + screen.getXSize(), screen.getGuiTop(), 48, 100));
            }
        });
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(SimpleByteStorage.CRAFTING_TERMINAL_BLOCK.get()), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new CraftingTerminalTransferHandler(registration.getTransferHelper()),
                RecipeTypes.CRAFTING
        );
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.CRAFTING, false), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.SMELTING, true), RecipeTypes.SMELTING);
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.BLASTING, true), RecipeTypes.BLASTING);
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.SMOKING, true), RecipeTypes.SMOKING);
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.STONECUTTING, true), RecipeTypes.STONECUTTING);
    }
}
