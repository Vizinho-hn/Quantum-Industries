package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.CopperCableBlockEntity;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
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

/** Cable graph endpoints are traversed by the source, never manufacture FE. */
public final class CopperCableBlock extends BaseEntityBlock {
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

    private static final VoxelShape CORE_SHAPE = Block.box(5.25, 5.25, 5.25, 10.75, 10.75, 10.75);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = Map.of(
            Direction.NORTH, Block.box(5.375, 5.375, 0, 10.625, 10.625, 5.25),
            Direction.SOUTH, Block.box(5.375, 5.375, 10.75, 10.625, 10.625, 16),
            Direction.WEST, Block.box(0, 5.375, 5.375, 5.25, 10.625, 10.625),
            Direction.EAST, Block.box(10.75, 5.375, 5.375, 16, 10.625, 10.625),
            Direction.UP, Block.box(5.375, 10.75, 5.375, 10.625, 16, 10.625),
            Direction.DOWN, Block.box(5.375, 0, 5.375, 10.625, 5.25, 10.625));

    private final boolean insulated;

    public CopperCableBlock(Properties properties, boolean insulated) {
        super(properties);
        this.insulated = insulated;
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    public boolean insulated() { return insulated; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return computeState(level, pos, state);
    }

    public static BlockState computeState(BlockGetter level, BlockPos pos, BlockState base) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            boolean loaded = !(level instanceof Level world) || world.hasChunkAt(neighbor);
            boolean connected = loaded && (level.getBlockState(neighbor).getBlock() instanceof CopperCableBlock
                    || (isMachine(level, neighbor) && br.com.quantumindustries.energy.EnergyPorts.connects(
                            level.getBlockState(neighbor), direction.getOpposite())));
            base = base.setValue(PROPERTY_BY_DIRECTION.get(direction), connected);
        }
        return base;
    }

    private static boolean isMachine(BlockGetter level, BlockPos nPos) {
        BlockState nState = level.getBlockState(nPos);
        Block nBlock = nState.getBlock();
        if (nBlock instanceof CopperCableBlock) return false;
        if (nBlock instanceof ManualDynamoBlock
                || nBlock instanceof VoltaicPileBlock
                || nBlock instanceof LeadAcidAccumulatorBlock
                || nBlock instanceof ElectromechanicalPressBlock
                || nBlock instanceof PiezoSwitchBlock
                || nBlock instanceof LoadRelayBlock
                || nBlock instanceof WireDrawingBenchBlock
                || nBlock instanceof PeristalticPumpBlock
                || nBlock instanceof BellowsEjectorBlock
                || nBlock instanceof ElectricAlloySmelterBlock
                || nBlock instanceof JawCrusherBlock) {
            return true;
        }
        return level.getBlockEntity(nPos) instanceof EnergyInspectable;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(() -> new CopperCableBlock(Properties.of().strength(0.8F).noOcclusion(), insulated));
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected net.minecraft.world.item.ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new net.minecraft.world.item.ItemStack(insulated
                ? br.com.quantumindustries.ModItems.INSULATED_COPPER_WIRE.get()
                : br.com.quantumindustries.ModItems.COPPER_WIRE.get());
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE_SHAPE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(direction))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(direction));
            }
        }
        return shape;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperCableBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.COPPER_CABLE_ENTITY.get(),
                (l, p, s, entity) -> entity.tick());
    }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
        if (insulated || !(level instanceof ServerLevel server) || !(entity instanceof LivingEntity living)
                || !living.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                || server.getGameTime() % 20 != 0) return;
        if (server.getBlockEntity(pos) instanceof CopperCableBlockEntity cable && cable.flow() > 0) {
            living.hurtServer(server, server.damageSources().lightningBolt(), 1.0F);
        }
    }
}
