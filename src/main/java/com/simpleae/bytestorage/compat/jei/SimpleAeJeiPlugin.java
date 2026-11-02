package com.simpleae.bytestorage.compat.jei;

import com.simpleae.bytestorage.SimpleByteStorage;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

/** Плагін JEI: «+» біля рецептів переносить їх у Термінал Кодування Шаблонів. */
@JeiPlugin
public class SimpleAeJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(SimpleByteStorage.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.CRAFTING, false), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.SMELTING, true), RecipeTypes.SMELTING);
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.BLASTING, true), RecipeTypes.BLASTING);
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.SMOKING, true), RecipeTypes.SMOKING);
        registration.addRecipeTransferHandler(new EncoderTransferHandler<>(RecipeTypes.STONECUTTING, true), RecipeTypes.STONECUTTING);
    }
}
