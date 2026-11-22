package com.simpleae.bytestorage.dimension.item;

import com.simpleae.bytestorage.block.CableBlock;
import com.simpleae.bytestorage.block.entity.CableBlockEntity;
import com.simpleae.bytestorage.item.CableFacadeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Сканер фасадов: многоразовый инструмент для массовой маскировки кабелей сети.
 * - Shift+ПКМ по целому блоку — скопировать внешний вид.
 * - ПКМ по кабелю — применить фасад сразу к цепочке до 32 соединённых кабелей.
 * - Shift+ПКМ по замаскированному кабелю — очистить фасад.
 */
public class FacadeScannerItem extends Item {

    private static final int MAX_CHAIN = 32;

    public FacadeScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);

        if (player.isShiftKeyDown()) {
            if (be instanceof CableBlockEntity cable) {
                if (cable.getFacade() != null) {
                    if (!level.isClientSide) {
                        cable.setFacade(null);
                        player.displayClientMessage(
                                Component.translatable("msg.simple_bytestorage_dimension.facade_scanner.cleared")
                                        .withStyle(ChatFormatting.YELLOW), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                return InteractionResult.PASS;
            }
            if (clicked.getBlock() instanceof CableBlock || clicked.getRenderShape() != RenderShape.MODEL
                    || !clicked.isSolidRender(level, pos)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                CableFacadeItem.setSavedState(stack, clicked);
                player.displayClientMessage(
                        Component.translatable("msg.simple_bytestorage_dimension.facade_scanner.copied",
                                clicked.getBlock().getName()).withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (be instanceof CableBlockEntity startCable) {
            BlockState facade = CableFacadeItem.getSavedState(stack);
            if (facade == null) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("msg.simple_bytestorage_dimension.facade_scanner.empty")
                                    .withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResult.CONSUME;
            }
            if (!level.isClientSide) {
                BlockState targetInitial = startCable.getFacade();
                Queue<BlockPos> queue = new ArrayDeque<>();
                Set<BlockPos> visited = new HashSet<>();
                queue.add(pos);
                visited.add(pos);
                int applied = 0;

                while (!queue.isEmpty() && applied < MAX_CHAIN) {
                    BlockPos cur = queue.poll();
                    if (level.getBlockEntity(cur) instanceof CableBlockEntity cable
                            && Objects.equals(cable.getFacade(), targetInitial)) {
                        cable.setFacade(facade);
                        applied++;
                        for (Direction dir : Direction.values()) {
                            BlockPos next = cur.relative(dir);
                            if (visited.add(next) && level.getBlockEntity(next) instanceof CableBlockEntity) {
                                queue.add(next);
                            }
                        }
                    }
                }
                player.displayClientMessage(
                        Component.translatable("msg.simple_bytestorage_dimension.facade_scanner.applied",
                                applied, facade.getBlock().getName()).withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BlockState saved = CableFacadeItem.getSavedState(stack);
        if (saved != null) {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.facade_scanner.bound",
                    saved.getBlock().getName()).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.facade_scanner.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.simple_bytestorage_dimension.facade_scanner.help")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
