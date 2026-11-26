package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.GasTransporterBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GasTransporterBlock extends BaseEntityBlock {
    public GasTransporterBlock(Properties properties) {
        super(properties);
        var state = stateDefinition.any();
        for (var property : FluidPipeBlock.PROPERTY_BY_DIRECTION.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    @Override protected MapCodec<GasTransporterBlock> codec() { return simpleCodec(GasTransporterBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        FluidPipeBlock.PROPERTY_BY_DIRECTION.values().forEach(builder::add);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasTransporterBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.GAS_TRANSPORTER_ENTITY.get(),
                (l, p, s, pipe) -> pipe.tick());
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connected(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return connected(level, pos, state);
    }
    public static BlockState connected(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction side : Direction.values()) {
            var neighbor = pos.relative(side);
            boolean connected = false;
            if (!(level instanceof Level world) || world.hasChunkAt(neighbor)) {
                var own = ConfigurableConduit.modeAt(level, pos, side);
                var face = side.getOpposite();
                var other = level.getBlockState(neighbor);
                if (other.getBlock() instanceof GasTransporterBlock) {
                    var mode = ConfigurableConduit.modeAt(level, neighbor, face);
                    connected = own.allowsOutput() && mode.allowsInput() || own.allowsInput() && mode.allowsOutput();
                } else if (other.getBlock() instanceof IndustrialConversionBlock machine) {
                    var facing = IndustrialConversionBlock.facing(other);
                    connected = machine.kind() == IndustrialConversionBlock.Kind.BOILER
                            && face == facing && own.allowsInput()
                            || machine.kind() == IndustrialConversionBlock.Kind.STEAM_ENGINE
                            && face == facing.getOpposite() && own.allowsOutput();
                }
            }
            state = state.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side), connected);
        }
        return state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Block.box(4, 4, 4, 12, 12, 12);
        for (Direction side : Direction.values()) {
            if (!state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side))) continue;
            shape = Shapes.or(shape, switch (side) {
                case NORTH -> Block.box(5, 5, 0, 11, 11, 4);
                case SOUTH -> Block.box(5, 5, 12, 11, 11, 16);
                case WEST -> Block.box(0, 5, 5, 4, 11, 11);
                case EAST -> Block.box(12, 5, 5, 16, 11, 11);
                case DOWN -> Block.box(5, 0, 5, 11, 4, 11);
                case UP -> Block.box(5, 12, 5, 11, 16, 11);
            });
        }
        return shape;
    }
}
