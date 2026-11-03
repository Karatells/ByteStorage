package com.simpleae.bytestorage.block;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Блок Контролера мережі Simple Byte Storage.
 * Центр живлення та керування мережею.
 */
public class NetworkControllerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<GridState> STATE = EnumProperty.create("state", GridState.class);

    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    public NetworkControllerBlock(Properties properties) {
        super(properties.lightLevel(state -> {
            GridState s = state.getValue(STATE);
            return switch (s) {
                case ONLINE -> 10;
                case CONFLICT -> 7;
                case OFFLINE -> 0;
            };
        }));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STATE, GridState.OFFLINE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(STATE, GridState.OFFLINE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, SimpleByteStorage.CONTROLLER_BLOCK_ENTITY.get(), NetworkControllerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NetworkControllerBlockEntity controller) {
                GridState gridState = state.getValue(STATE);
                int energy = controller.getEnergyStored();
                int maxEnergy = controller.getMaxEnergyStored();
                int drain = controller.getGrid() != null ? controller.getGrid().getTotalEnergyDrain() : 0;
                int nodesCount = controller.getGrid() != null ? controller.getGrid().getNodes().size() : 1;

                ChatFormatting statusColor = switch (gridState) {
                    case ONLINE -> ChatFormatting.GREEN;
                    case CONFLICT -> ChatFormatting.RED;
                    case OFFLINE -> ChatFormatting.GRAY;
                };

                player.sendSystemMessage(Component.literal("=== [ Simple Byte Storage Network Controller ] ===").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("Статус мережі: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(gridState.name()).withStyle(statusColor, ChatFormatting.BOLD)));

                if (gridState == GridState.CONFLICT) {
                    player.sendSystemMessage(Component.literal("ПОМИЛКА: Виявлено кілька контролерів в одній мережі!").withStyle(ChatFormatting.RED));
                }

                player.sendSystemMessage(Component.literal("Енергія: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(FORMAT.format(energy) + " / " + FORMAT.format(maxEnergy) + " FE").withStyle(ChatFormatting.GOLD)));
                player.sendSystemMessage(Component.literal("Споживання: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(FORMAT.format(drain) + " FE/t").withStyle(ChatFormatting.YELLOW)));
                player.sendSystemMessage(Component.literal("Підключено пристроїв: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.valueOf(nodesCount)).withStyle(ChatFormatting.WHITE)));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
