package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.FluidPipeBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.fluid.FluidInspectable;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

public final class FluidPipeBlock extends BaseEntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.NORTH, NORTH, Direction.SOUTH, SOUTH,
            Direction.EAST, EAST, Direction.WEST, WEST,
            Direction.UP, UP, Direction.DOWN, DOWN);

    private static final VoxelShape CORE_SHAPE = Block.box(4.6, 4.6, 4.6, 11.4, 11.4, 11.4);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = Map.of(
            Direction.NORTH, Block.box(4.6, 4.6, 0, 11.4, 11.4, 4.6),
            Direction.SOUTH, Block.box(4.6, 4.6, 11.4, 11.4, 11.4, 16),
            Direction.WEST, Block.box(0, 4.6, 4.6, 4.6, 11.4, 11.4),
            Direction.EAST, Block.box(11.4, 4.6, 4.6, 16, 11.4, 11.4),
            Direction.UP, Block.box(4.6, 11.4, 4.6, 11.4, 16, 11.4),
            Direction.DOWN, Block.box(4.6, 0, 4.6, 11.4, 4.6, 11.4));

    private final boolean glass;
    public FluidPipeBlock(Properties properties, boolean glass) {
        super(properties);
        this.glass = glass;
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return computeState(level, pos, state);
    }

    public static BlockState computeState(BlockGetter level, BlockPos pos, BlockState base) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            boolean loaded = !(level instanceof Level world) || world.hasChunkAt(neighbor);
            boolean connected = loaded && canConnect(level, neighbor, direction.getOpposite());
            if (connected) {
                var ownMode = ConfigurableConduit.modeAt(level, pos, direction);
                var otherMode = ConfigurableConduit.modeAt(level, neighbor, direction.getOpposite());
                connected = ownMode.allowsInput() && otherMode.allowsOutput()
                        || ownMode.allowsOutput() && otherMode.allowsInput();
            }
            base = base.setValue(PROPERTY_BY_DIRECTION.get(direction), connected);
        }
        return base;
    }

    private static boolean canConnect(BlockGetter level, BlockPos neighbor, Direction fromSide) {
        BlockState state = level.getBlockState(neighbor);
        if (state.getBlock() instanceof FluidPipeBlock) return true;
        if (state.getBlock() instanceof PeristalticPumpBlock) {
            return fromSide.getAxis() == state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis();
        }
        if (level.getBlockEntity(neighbor) instanceof FluidInspectable) return true;
        return level instanceof Level world && world.getCapability(Capabilities.Fluid.BLOCK, neighbor, fromSide) != null;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec(){
        return MapCodec.unit(()->new FluidPipeBlock(Properties.of().strength(1.0f).noOcclusion(),glass));
    }
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        VoxelShape shape = CORE_SHAPE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(direction))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(direction));
            }
        }
        return shape;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FluidPipeBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide()?null:createTickerHelper(type,ModBlocks.FLUID_PIPE_ENTITY.get(),(l,p,s,be)->be.tick());
    }
}
