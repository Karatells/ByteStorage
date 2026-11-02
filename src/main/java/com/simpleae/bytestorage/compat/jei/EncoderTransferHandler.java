package com.simpleae.bytestorage.compat.jei;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.menu.PatternEncodingTerminalMenu;
import com.simpleae.bytestorage.network.packet.EncoderFillPacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Кнопка «+» у JEI: переносить рецепт у Термінал Кодування.
 * Верстак → режим «Верстак» (сітка 3×3), інші типи (піч тощо) → режим «Процес» (входи + результат).
 */
public class EncoderTransferHandler<R> implements IRecipeTransferHandler<PatternEncodingTerminalMenu, R> {

    private final RecipeType<R> recipeType;
    private final boolean processing;

    public EncoderTransferHandler(RecipeType<R> recipeType, boolean processing) {
        this.recipeType = recipeType;
        this.processing = processing;
    }

    public Class<? extends PatternEncodingTerminalMenu> getContainerClass() {
        return PatternEncodingTerminalMenu.class;
    }

    public Optional<MenuType<PatternEncodingTerminalMenu>> getMenuType() {
        return Optional.of(SimpleByteStorage.PATTERN_ENCODING_TERMINAL_MENU.get());
    }

    public RecipeType<R> getRecipeType() {
        return recipeType;
    }

    @Nullable
    public IRecipeTransferError transferRecipe(PatternEncodingTerminalMenu menu, R recipe, IRecipeSlotsView recipeSlots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) {
            return null;
        }
        List<ItemStack> inputs = new ArrayList<>();
        for (IRecipeSlotView view : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            Optional<ItemStack> shown = view.getDisplayedItemStack();
            inputs.add(shown.isPresent() ? shown.get().copy() : ItemStack.EMPTY);
        }
        ItemStack output = ItemStack.EMPTY;
        for (IRecipeSlotView view : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
            Optional<ItemStack> shown = view.getDisplayedItemStack();
            if (shown.isPresent()) {
                output = shown.get().copy();
                break;
            }
        }
        SimpleByteStoragePackets.sendToServer(new EncoderFillPacket(processing, inputs, output));
        return null;
    }

    @Nullable
    public IRecipeTransferError handleRecipe(PatternEncodingTerminalMenu menu, R recipe, IRecipeSlotsView recipeSlots,
                                             Player player, boolean maxTransfer, boolean doTransfer) {
        return transferRecipe(menu, recipe, recipeSlots, player, maxTransfer, doTransfer);
    }
}
