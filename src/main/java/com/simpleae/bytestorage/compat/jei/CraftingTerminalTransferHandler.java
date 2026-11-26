package com.simpleae.bytestorage.compat.jei;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.menu.CraftingTerminalMenu;
import com.simpleae.bytestorage.network.packet.CraftingTerminalJeiFillPacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Кнопка «+» у JEI для Термінала Крафту (CraftingTerminalMenu):
 * перевіряє наявність інгредієнтів у мережі + інвентарі гравця + крафтовій сітці
 * та заповнює сітку 3×3 прямо з мережі й інвентаря.
 */
public class CraftingTerminalTransferHandler implements IRecipeTransferHandler<CraftingTerminalMenu, CraftingRecipe> {

    private final IRecipeTransferHandlerHelper transferHelper;

    public CraftingTerminalTransferHandler(IRecipeTransferHandlerHelper transferHelper) {
        this.transferHelper = transferHelper;
    }

    @Override
    public Class<? extends CraftingTerminalMenu> getContainerClass() {
        return CraftingTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingTerminalMenu>> getMenuType() {
        return Optional.of(SimpleByteStorage.CRAFTING_TERMINAL_MENU.get());
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(CraftingTerminalMenu menu, CraftingRecipe recipe,
                                               IRecipeSlotsView recipeSlots, Player player,
                                               boolean maxTransfer, boolean doTransfer) {
        List<IRecipeSlotView> inputViews = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        int slotCount = Math.min(9, inputViews.size());

        // Збираємо пул доступних предметів (інвентар гравця + поточна крафтова сітка + мережа)
        List<ItemStack> available = new ArrayList<>();
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack st = player.getInventory().items.get(i);
            if (!st.isEmpty()) addAvailable(available, st);
        }
        for (int i = 0; i < menu.getCraftMatrix().getContainerSize(); i++) {
            ItemStack st = menu.getCraftMatrix().getItem(i);
            if (!st.isEmpty()) addAvailable(available, st);
        }
        for (ItemStack st : menu.getClientVirtualPool()) {
            if (!st.isEmpty()) addAvailable(available, st);
        }

        List<IRecipeSlotView> missingSlots = new ArrayList<>();
        List<List<ItemStack>> slotCandidates = new ArrayList<>(9);

        for (int i = 0; i < 9; i++) {
            if (i >= slotCount) {
                slotCandidates.add(List.of());
                continue;
            }
            IRecipeSlotView view = inputViews.get(i);
            List<ItemStack> candidates = view.getItemStacks().map(ItemStack::copy).toList();
            slotCandidates.add(candidates);
            if (candidates.isEmpty()) {
                continue;
            }
            if (!consumeOneMatch(available, candidates)) {
                missingSlots.add(view);
            }
        }

        if (!missingSlots.isEmpty()) {
            return transferHelper.createUserErrorForMissingSlots(
                    Component.translatable("jei.tooltip.error.recipe.transfer.missing"),
                    missingSlots
            );
        }

        if (doTransfer) {
            SimpleByteStoragePackets.sendToServer(new CraftingTerminalJeiFillPacket(slotCandidates, maxTransfer));
        }
        return null;
    }

    private static void addAvailable(List<ItemStack> pool, ItemStack stack) {
        for (ItemStack existing : pool) {
            if (ItemStack.isSameItemSameTags(existing, stack)) {
                existing.grow(stack.getCount());
                return;
            }
        }
        pool.add(stack.copy());
    }

    private static boolean consumeOneMatch(List<ItemStack> pool, List<ItemStack> candidates) {
        for (ItemStack cand : candidates) {
            if (cand.isEmpty()) continue;
            for (ItemStack avail : pool) {
                if (!avail.isEmpty() && matchesCandidate(avail, cand)) {
                    avail.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesCandidate(ItemStack avail, ItemStack cand) {
        if (!avail.is(cand.getItem())) return false;
        if (!cand.hasTag()) return true;
        return ItemStack.isSameItemSameTags(avail, cand);
    }
}
