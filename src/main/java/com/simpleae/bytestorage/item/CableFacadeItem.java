package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.block.CableBlock;
import com.simpleae.bytestorage.block.entity.CableBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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

import java.util.List;

/**
 * Фасад кабеля: позволяет замаскировать Network Cable под любой полный строительный блок.
 * Shift+ПКМ по блоку — скопировать внешний вид.
 * ПКМ по кабелю — применить фасад.
 */
public class CableFacadeItem extends Item {

    public static final String TAG_FACADE = "FacadeState";

    public CableFacadeItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static BlockState getSavedState(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_FACADE)) {
            return null;
        }
        BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stack.getTag().getCompound(TAG_FACADE));
        return state.isAir() ? null : state;
    }

    public static void setSavedState(ItemStack stack, BlockState state) {
        stack.getOrCreateTag().put(TAG_FACADE, NbtUtils.writeBlockState(state));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = ctx.getItemInHand();
        BlockState clicked = level.getBlockState(ctx.getClickedPos());

        if (player.isShiftKeyDown()) {
            if (clicked.getBlock() instanceof CableBlock || clicked.getRenderShape() != RenderShape.MODEL
                    || !clicked.isSolidRender(level, ctx.getClickedPos())) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                setSavedState(stack, clicked);
                player.displayClientMessage(
                        Component.translatable("msg.simple_bytestorage.facade.copied",
                                clicked.getBlock().getName()).withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (be instanceof CableBlockEntity cable) {
            BlockState facade = getSavedState(stack);
            if (facade == null) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("msg.simple_bytestorage.facade.empty")
                                    .withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResult.CONSUME;
            }
            if (!level.isClientSide) {
                cable.setFacade(facade);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        BlockState state = getSavedState(stack);
        if (state != null) {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage.facade.bound",
                    state.getBlock().getName()).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage.facade.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
