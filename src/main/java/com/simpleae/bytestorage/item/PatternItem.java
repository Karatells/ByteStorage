package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Предмет-шаблон крафту.
 *
 * Може бути «чистим» (без рецепту) або «закодованим». Закодований шаблон зберігає у NBT
 * повний рецепт: сітку 3×3 та результат. Той самий рецепт паралельно потрапляє у глобальний
 * список шаблонів мережі (Термінал Шаблонів).
 */
public class PatternItem extends Item {

    public static final String TAG_PROGRAMMED = "PatternProgrammed";
    public static final String TAG_ID = "PatternId";
    public static final String TAG_OUTPUT = "PatternOutput";
    public static final String TAG_GRID = "PatternGrid";
    public static final String TAG_PROCESSING = "PatternProcessing";

    public PatternItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // --- Стан ---

    public boolean isProgrammed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_PROGRAMMED);
    }

    /**
     * Прочитати рецепт із шаблону; null — якщо шаблон чистий або пошкоджений.
     */
    @Nullable
    public PatternData getPattern(ItemStack stack) {
        if (!isProgrammed(stack)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_OUTPUT, 10) || !tag.contains(TAG_GRID, 9)) {
            return null;
        }
        ItemStack output = ItemStack.of(tag.getCompound(TAG_OUTPUT));
        if (output.isEmpty()) {
            return null;
        }
        ListTag list = tag.getList(TAG_GRID, 10);
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            grid.add(ItemStack.of(list.getCompound(i)));
        }
        if (tag.getBoolean(TAG_PROCESSING)) {
            return PatternData.processing(output, grid);
        }
        return new PatternData(output, grid);
    }

    /**
     * Записати рецепт у шаблон.
     */
    public void program(ItemStack stack, PatternData pattern) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_PROGRAMMED, true);
        tag.putString(TAG_ID, pattern.getId());
        tag.putBoolean(TAG_PROCESSING, pattern.isProcessing());
        tag.put(TAG_OUTPUT, pattern.getOutput().save(new CompoundTag()));
        ListTag list = new ListTag();
        for (ItemStack cell : pattern.getGrid()) {
            list.add(cell.save(new CompoundTag()));
        }
        tag.put(TAG_GRID, list);
        stack.setCount(1);
    }

    /**
     * Повернути шаблон до чистого стану.
     */
    public void clear(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_PROGRAMMED);
            tag.remove(TAG_ID);
            tag.remove(TAG_PROCESSING);
            tag.remove(TAG_OUTPUT);
            tag.remove(TAG_GRID);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    // --- Назва та тултіп ---

    @Override
    public Component getName(ItemStack stack) {
        PatternData pattern = getPattern(stack);
        if (pattern != null) {
            return Component.translatable("item.simple_bytestorage.pattern.encoded",
                    pattern.getOutput().getHoverName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        PatternData pattern = getPattern(stack);
        if (pattern == null) {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage.pattern.blank")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }

        if (pattern.isProcessing()) {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage.pattern.processing")
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.simple_bytestorage.pattern.result")
                .withStyle(ChatFormatting.GRAY)
                .append(pattern.getOutput().getHoverName().copy().withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" x" + pattern.getOutput().getCount()).withStyle(ChatFormatting.GOLD)));

        tooltip.add(Component.translatable("tooltip.simple_bytestorage.pattern.ingredients")
                .withStyle(ChatFormatting.YELLOW));
        for (ItemStack input : pattern.getInputs()) {
            tooltip.add(Component.literal("  • ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(input.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" x" + input.getCount()).withStyle(ChatFormatting.GOLD)));
        }
    }
}
