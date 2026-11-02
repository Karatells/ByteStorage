package com.simpleae.bytestorage.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Допоміжні методи для виконання ванільних рецептів верстака (3×3) без реального верстака.
 * Використовується і Терміналом Кодування (щоб визначити результат шаблону),
 * і Асемблером (щоб справді виконати крафт).
 */
public final class PatternCrafting {

    /** «Фіктивне» меню: TransientCraftingContainer вимагає меню, щоб повідомляти про зміни слотів. */
    private static final AbstractContainerMenu DUMMY_MENU = new AbstractContainerMenu(null, -1) {
        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }
    };

    private PatternCrafting() {
    }

    /** Створює контейнер 3×3 із копіями переданих стаків (9 елементів, ряд за рядом). */
    public static TransientCraftingContainer makeContainer(List<ItemStack> grid) {
        TransientCraftingContainer container = new TransientCraftingContainer(DUMMY_MENU, 3, 3);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
            container.setItem(i, stack.copy());
        }
        return container;
    }

    public static Optional<CraftingRecipe> findRecipe(Level level, TransientCraftingContainer container) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level);
    }

    /** Результат одного крафту за сіткою; EMPTY — якщо такого рецепту немає. */
    public static ItemStack computeResult(Level level, List<ItemStack> grid) {
        TransientCraftingContainer container = makeContainer(grid);
        Optional<CraftingRecipe> recipe = findRecipe(level, container);
        if (recipe.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return recipe.get().assemble(container, level.registryAccess());
    }

    /** «Залишки» рецепту (відра, пляшки тощо), які повертаються після крафту. */
    public static NonNullList<ItemStack> remainingItems(CraftingRecipe recipe, TransientCraftingContainer container) {
        return recipe.getRemainingItems(container);
    }
}
