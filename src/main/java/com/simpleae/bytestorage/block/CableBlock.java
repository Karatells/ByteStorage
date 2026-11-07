package com.simpleae.bytestorage.block;

import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.block.entity.CableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Кабель: тонкий провід, що з'єднує будь-які ноди мережі (мережа знаходить ноди через сусідство,
 * кабель просто подовжує ланцюг). Рукави моделі показують, з чим кабель з'єднаний.
 */
public class CableBlock extends BaseEntityBlock {

    public static final Map<Direction, BooleanProperty> PROPERTIES = new EnumMap<>(Direction.class);

    static {
        PROPERTIES.put(Direction.NORTH, BlockStateProperties.NORTH);
        PROPERTIES.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        PROPERTIES.put(Direction.EAST, BlockStateProperties.EAST);
        PROPERTIES.put(Direction.WEST, BlockStateProperties.WEST);
        PROPERTIES.put(Direction.UP, BlockStateProperties.UP);
        PROPERTIES.put(Direction.DOWN, BlockStateProperties.DOWN);
    }

    /** Штекер: ставится, когда кабель подключён к блоку-устройству (а не к другому кабелю). */
    public static final Map<Direction, BooleanProperty> PLUGS = new EnumMap<>(Direction.class);

    static {
        for (Direction direction : Direction.values()) {
            PLUGS.put(direction, BooleanProperty.create("plug_" + direction.getName()));
        }
    }

    private static final Map<Direction, VoxelShape> PLUG_SHAPES = new EnumMap<>(Direction.class);

    static {
        PLUG_SHAPES.put(Direction.NORTH, Block.box(3, 3, 0, 13, 13, 3));
        PLUG_SHAPES.put(Direction.SOUTH, Block.box(3, 3, 13, 13, 13, 16));
        PLUG_SHAPES.put(Direction.WEST, Block.box(0, 3, 3, 3, 13, 13));
        PLUG_SHAPES.put(Direction.EAST, Block.box(13, 3, 3, 16, 13, 13));
        PLUG_SHAPES.put(Direction.DOWN, Block.box(3, 0, 3, 13, 3, 13));
        PLUG_SHAPES.put(Direction.UP, Block.box(3, 13, 3, 13, 16, 13));
    }

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
        ARMS.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
        ARMS.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
        ARMS.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
        ARMS.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
        ARMS.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
    }

    public CableBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : PROPERTIES.values()) {
            state = state.setValue(property, false);
        }
        for (BooleanProperty property : PLUGS.values()) {
            state = state.setValue(property, false);
        }
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (BooleanProperty property : PROPERTIES.values()) {
            builder.add(property);
        }
        for (BooleanProperty property : PLUGS.values()) {
            builder.add(property);
        }
    }

    private static boolean connectsTo(LevelReader level, BlockPos pos, Direction direction) {
        BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
        return neighbor instanceof IGridNode;
    }

    private BlockState withConnections(LevelReader level, BlockPos pos, BlockState state) {
        for (Map.Entry<Direction, BooleanProperty> entry : PROPERTIES.entrySet()) {
            Direction direction = entry.getKey();
            boolean connected = connectsTo(level, pos, direction);
            state = state.setValue(entry.getValue(), connected);
            state = state.setValue(PLUGS.get(direction), connected
                    && !(level.getBlockState(pos.relative(direction)).getBlock() instanceof CableBlock));
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean connected = connectsTo(level, pos, direction);
        return state.setValue(PROPERTIES.get(direction), connected)
                .setValue(PLUGS.get(direction), connected && !(neighborState.getBlock() instanceof CableBlock));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Map.Entry<Direction, BooleanProperty> entry : PROPERTIES.entrySet()) {
            if (state.getValue(entry.getValue())) {
                shape = Shapes.or(shape, ARMS.get(entry.getKey()));
                if (state.getValue(PLUGS.get(entry.getKey()))) {
                    shape = Shapes.or(shape, PLUG_SHAPES.get(entry.getKey()));
                }
            }
        }
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }
}
