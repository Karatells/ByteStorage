package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Полевой сканер сети: ПКМ по блоку сети выводит диагностику (статус, узлы, FE/т, память). */
public class NetworkAnalyzerItem extends Item {

    public NetworkAnalyzerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof IGridNode node)) {
            player.displayClientMessage(
                    Component.translatable("msg.simple_bytestorage.analyzer.not_node").withStyle(ChatFormatting.YELLOW),
                    true);
            return InteractionResult.SUCCESS;
        }
        IGrid grid = node.getGrid();
        if (grid == null) {
            player.displayClientMessage(
                    Component.translatable("msg.simple_bytestorage.analyzer.no_grid").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.SUCCESS;
        }
        NetworkControllerBlockEntity controller = grid.getController();
        int energy = controller != null ? controller.getEnergyStored() : 0;
        int maxEnergy = controller != null ? controller.getMaxEnergyStored() : 0;
        int used = grid.getStorage() != null ? grid.getStorage().getTotalUsedBytes() : 0;
        int cap = grid.getStorage() != null ? grid.getStorage().getTotalCapacityBytes() : 0;

        player.sendSystemMessage(Component.translatable(
                "msg.simple_bytestorage.analyzer.report",
                grid.getState().name(),
                grid.getNodes().size(),
                node.getIdleEnergyDrain(),
                grid.getTotalEnergyDrain(),
                energy, maxEnergy,
                used, cap
        ).withStyle(ChatFormatting.AQUA));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.simple_bytestorage.network_analyzer")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
