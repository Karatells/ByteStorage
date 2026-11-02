package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.item.PatternItem;
import com.simpleae.bytestorage.menu.PatternEncodingTerminalMenu;
import com.simpleae.bytestorage.util.PatternCrafting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Термінал Кодування Шаблонів.
 *
 * Гравець вставляє шаблон, «малює» рецепт у сітці 3×3 (слоти-привиди: предмети не витрачаються),
 * результат визначається автоматично за ванільними рецептами. Кнопка «Записати» записує рецепт
 * у предмет-шаблон та додає його у глобальний список шаблонів мережі.
 */
public class PatternEncodingTerminalBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    // Слоти внутрішнього інвентаря
    public static final int BLANK_PATTERN_SLOT = 0;      // вхід: шаблон (чистий або для перезапису)
    public static final int PROGRAMMED_PATTERN_SLOT = 1; // вихід: закодований шаблон
    public static final int RECIPE_GRID_START = 2;       // 2..10 — сітка 3×3 (слоти-привиди)
    public static final int RECIPE_GRID_SIZE = 9;
    public static final int RESULT_PREVIEW_SLOT = 11;    // лише відображення результату рецепту
    private static final int INV_SIZE = 12;

    private IGrid grid;

    /** Режим: false — крафт верстака (в глобальний список), true — процесний шаблон для Шини Шаблонів. */
    private boolean processing = false;

    private final ItemStackHandler inventory = new ItemStackHandler(INV_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!processing && slot >= RECIPE_GRID_START && slot < RECIPE_GRID_START + RECIPE_GRID_SIZE) {
                updateResultPreview();
            }
        }
    };

    public PatternEncodingTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.PATTERN_ENCODING_TERMINAL_BLOCK_ENTITY.get(), pos, state);
    }

    // --- IGridNode ---

    @Override
    @Nullable
    public IGrid getGrid() {
        return grid;
    }

    @Override
    public void setGrid(@Nullable IGrid grid) {
        this.grid = grid;
    }

    @Override
    public BlockPos getNodePos() {
        return this.worldPosition;
    }

    @Override
    public Level getNodeLevel() {
        return this.level;
    }

    @Override
    public int getIdleEnergyDrain() {
        return 2;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.pattern_encoding_terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PatternEncodingTerminalMenu(containerId, playerInventory, this);
    }

    // --- Допоміжне ---

    public IItemHandler getInventoryHandler() {
        return inventory;
    }

    public List<ItemStack> getRecipeGrid() {
        List<ItemStack> result = new ArrayList<>(RECIPE_GRID_SIZE);
        for (int i = 0; i < RECIPE_GRID_SIZE; i++) {
            result.add(inventory.getStackInSlot(RECIPE_GRID_START + i));
        }
        return result;
    }

    /** Перерахувати результат рецепту (лише на сервері; клієнт отримує його через синхронізацію слотів). */
    private void updateResultPreview() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack result = PatternCrafting.computeResult(level, getRecipeGrid());
        if (!ItemStack.matches(result, inventory.getStackInSlot(RESULT_PREVIEW_SLOT))) {
            inventory.setStackInSlot(RESULT_PREVIEW_SLOT, result);
        }
    }

    /** Очистити сітку рецепту (і результат у процесному режимі). */
    public void clearRecipeGrid() {
        for (int i = 0; i < RECIPE_GRID_SIZE; i++) {
            inventory.setStackInSlot(RECIPE_GRID_START + i, ItemStack.EMPTY);
        }
        inventory.setStackInSlot(RESULT_PREVIEW_SLOT, ItemStack.EMPTY);
    }

    public boolean isProcessing() {
        return processing;
    }

    /** Перемкнути режим кодування; сітка очищається, бо рецепти двох режимів несумісні. */
    public void toggleProcessing() {
        this.processing = !this.processing;
        clearRecipeGrid();
        setChanged();
    }

    /**
     * Записати рецепт у шаблон і додати його в глобальний список мережі.
     *
     * @return ключ повідомлення для гравця (success або причина відмови) та аргумент
     */
    public Component encodePattern() {
        if (level == null || level.isClientSide) {
            return Component.empty();
        }

        ItemStack template = inventory.getStackInSlot(BLANK_PATTERN_SLOT);
        if (template.isEmpty() || !(template.getItem() instanceof PatternItem patternItem)) {
            return Component.translatable("msg.simple_bytestorage.encode.no_pattern");
        }
        if (!inventory.getStackInSlot(PROGRAMMED_PATTERN_SLOT).isEmpty()) {
            return Component.translatable("msg.simple_bytestorage.encode.output_busy");
        }

        List<ItemStack> recipeGrid = getRecipeGrid();
        PatternData data;
        ItemStack output;
        if (processing) {
            output = inventory.getStackInSlot(RESULT_PREVIEW_SLOT).copy();
            if (output.isEmpty()) {
                return Component.translatable("msg.simple_bytestorage.encode.no_output");
            }
            boolean hasInput = false;
            for (ItemStack cell : recipeGrid) {
                hasInput |= !cell.isEmpty();
            }
            if (!hasInput) {
                return Component.translatable("msg.simple_bytestorage.encode.no_input");
            }
            // Процесний шаблон не потрапляє в глобальний список: його кладуть у Шину Шаблонів біля машини.
            data = PatternData.processing(output, recipeGrid);
        } else {
            output = PatternCrafting.computeResult(level, recipeGrid);
            if (output.isEmpty()) {
                return Component.translatable("msg.simple_bytestorage.encode.no_recipe");
            }
            if (grid == null || !grid.getState().isActive() || grid.getPatternStorage() == null) {
                return Component.translatable("msg.simple_bytestorage.encode.no_network");
            }
            data = grid.getPatternStorage().addPattern(output, recipeGrid);
        }

        ItemStack encoded = template.copy();
        encoded.setCount(1);
        patternItem.program(encoded, data);

        inventory.setStackInSlot(BLANK_PATTERN_SLOT, ItemStack.EMPTY);
        inventory.setStackInSlot(PROGRAMMED_PATTERN_SLOT, encoded);

        return Component.translatable(processing
                ? "msg.simple_bytestorage.encode.success_processing"
                : "msg.simple_bytestorage.encode.success", output.getHoverName());
    }

    /** Предмети, які треба повернути гравцеві при руйнуванні блоку. */
    public List<ItemStack> getDroppedItems() {
        List<ItemStack> drops = new ArrayList<>();
        ItemStack blank = inventory.getStackInSlot(BLANK_PATTERN_SLOT);
        ItemStack programmed = inventory.getStackInSlot(PROGRAMMED_PATTERN_SLOT);
        if (!blank.isEmpty()) {
            drops.add(blank.copy());
        }
        if (!programmed.isEmpty()) {
            drops.add(programmed.copy());
        }
        return drops;
    }

    // --- Збереження ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("EncoderInventory", inventory.serializeNBT());
        tag.putBoolean("Processing", processing);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        processing = tag.getBoolean("Processing");
        if (tag.contains("EncoderInventory")) {
            inventory.deserializeNBT(tag.getCompound("EncoderInventory"));
        }
    }
}
