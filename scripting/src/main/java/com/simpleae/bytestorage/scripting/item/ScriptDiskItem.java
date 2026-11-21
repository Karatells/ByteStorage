package com.simpleae.bytestorage.scripting.item;

import com.simpleae.bytestorage.block.entity.SecurityTerminalBlockEntity;
import com.simpleae.bytestorage.scripting.block.entity.LogicBusBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Диск скрипта: позволяет копировать скрипт с одной Логической шины на другую.
 * Shift+ПКМ по Logic Bus — скопировать скрипт на диск.
 * ПКМ по Logic Bus — записать скрипт с диска на шину.
 */
public class ScriptDiskItem extends Item {

    public static final String TAG_SCRIPT = "SavedScript";
    public static final String TAG_INTERVAL = "SavedInterval";
    public static final String TAG_SOURCE_ID = "SourceId";

    public ScriptDiskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean hasScript(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_SCRIPT);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof LogicBusBlockEntity bus)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        if (!SecurityTerminalBlockEntity.canAccess(bus.getGrid(), player)) {
            player.displayClientMessage(
                    Component.translatable("msg.simple_bytestorage.security.denied").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        ItemStack disk = ctx.getItemInHand();
        if (player.isShiftKeyDown()) {
            CompoundTag tag = disk.getOrCreateTag();
            tag.putString(TAG_SCRIPT, bus.getScript());
            tag.putInt(TAG_INTERVAL, bus.getInterval());
            tag.putString(TAG_SOURCE_ID, bus.getIdentifier());
            player.displayClientMessage(
                    Component.translatable("msg.simple_bytestorage_scripting.disk.copied").withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }
        if (!hasScript(disk)) {
            player.displayClientMessage(
                    Component.translatable("msg.simple_bytestorage_scripting.disk.empty").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        CompoundTag tag = disk.getTag();
        bus.setScript(tag.getString(TAG_SCRIPT));
        if (tag.contains(TAG_INTERVAL)) {
            bus.setInterval(tag.getInt(TAG_INTERVAL));
        }
        player.displayClientMessage(
                Component.translatable("msg.simple_bytestorage_scripting.disk.applied").withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (hasScript(stack)) {
            CompoundTag tag = stack.getTag();
            String script = tag.getString(TAG_SCRIPT);
            int lines = script.isEmpty() ? 0 : script.split("\n", -1).length;
            String srcId = tag.getString(TAG_SOURCE_ID);
            tooltip.add(Component.translatable("tooltip.simple_bytestorage_scripting.disk.saved",
                    lines, srcId.isEmpty() ? "-" : srcId).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.simple_bytestorage_scripting.disk.use")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage_scripting.disk.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
