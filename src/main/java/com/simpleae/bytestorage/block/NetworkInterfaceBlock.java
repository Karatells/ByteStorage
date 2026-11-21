package com.simpleae.bytestorage.block;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.block.entity.NetworkInterfaceBlockEntity;
import com.simpleae.bytestorage.block.entity.SecurityTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Сетевой интерфейс: универсальный мост для труб, воронок и механизмов других модов.
 * Вставка предметов идёт напрямую в сеть; буфер из 9 слотов автоматически поддерживается из сети по заданным шаблонам.
 */
public class NetworkInterfaceBlock extends BaseEntityBlock {

    public NetworkInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkInterfaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, SimpleByteStorage.NETWORK_INTERFACE_BLOCK_ENTITY.get(),
                        NetworkInterfaceBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NetworkInterfaceBlockEntity iface && player instanceof ServerPlayer serverPlayer) {
                if (!SecurityTerminalBlockEntity.canAccess(iface.getGrid(), serverPlayer)) {
                    serverPlayer.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("msg.simple_bytestorage.security.denied")
                                    .withStyle(net.minecraft.ChatFormatting.RED), true);
                    return InteractionResult.CONSUME;
                }
                NetworkHooks.openScreen(serverPlayer, iface, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NetworkInterfaceBlockEntity iface) {
                ItemStackHandler buf = iface.getBufferInventory();
                for (int i = 0; i < buf.getSlots(); i++) {
                    ItemStack stack = buf.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
