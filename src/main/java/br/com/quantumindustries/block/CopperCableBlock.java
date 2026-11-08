package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.CopperCableBlockEntity;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cable graph endpoints are traversed by the source, never manufacture FE. */
public final class CopperCableBlock extends BaseEntityBlock {
    private static final VoxelShape BOX_SHAPE = Block.box(4, 4, 4, 12, 12, 12);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<CableShape> SHAPE = EnumProperty.create("shape", CableShape.class);

    private final boolean insulated;

    public enum CableShape implements StringRepresentable {
        STRAIGHT("straight"),
        ELBOW("elbow"),
        TEE("tee"),
        CROSS("cross"),
        MACHINE("machine");

        private final String name;

        CableShape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public CopperCableBlock(Properties properties, boolean insulated) {
        super(properties);
        this.insulated = insulated;
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.EAST)
                .setValue(SHAPE, CableShape.STRAIGHT));
    }

    public boolean insulated() { return insulated; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction playerFacing = context.getHorizontalDirection().getOpposite();
        BlockState base = defaultBlockState().setValue(FACING, playerFacing);
        return computeState(context.getLevel(), context.getClickedPos(), base);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (direction.getAxis().isHorizontal()) {
            return computeState(level, pos, state);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    public static BlockState computeState(BlockGetter level, BlockPos pos, BlockState base) {
        boolean northCable = isCable(level, pos.north());
        boolean southCable = isCable(level, pos.south());
        boolean eastCable = isCable(level, pos.east());
        boolean westCable = isCable(level, pos.west());

        boolean northMachine = !northCable && isMachine(level, pos.north());
        boolean southMachine = !southCable && isMachine(level, pos.south());
        boolean eastMachine = !eastCable && isMachine(level, pos.east());
        boolean westMachine = !westCable && isMachine(level, pos.west());

        boolean north = northCable || northMachine;
        boolean south = southCable || southMachine;
        boolean east = eastCable || eastMachine;
        boolean west = westCable || westMachine;

        int count = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);

        if (count == 4) {
            return base.setValue(SHAPE, CableShape.CROSS).setValue(FACING, Direction.EAST);
        }

        if (count == 3) {
            Direction facing;
            if (!north) facing = Direction.EAST;
            else if (!east) facing = Direction.SOUTH;
            else if (!south) facing = Direction.WEST;
            else facing = Direction.NORTH;
            return base.setValue(SHAPE, CableShape.TEE).setValue(FACING, facing);
        }

        if (count == 2) {
            // Perpendicular elbows
            if (west && south) {
                return base.setValue(SHAPE, CableShape.ELBOW).setValue(FACING, Direction.EAST);
            }
            if (north && west) {
                return base.setValue(SHAPE, CableShape.ELBOW).setValue(FACING, Direction.SOUTH);
            }
            if (north && east) {
                return base.setValue(SHAPE, CableShape.ELBOW).setValue(FACING, Direction.WEST);
            }
            if (south && east) {
                return base.setValue(SHAPE, CableShape.ELBOW).setValue(FACING, Direction.NORTH);
            }

            // Collinear lines: West-East or North-South
            if (west && east) {
                if (eastMachine) {
                    return base.setValue(SHAPE, CableShape.MACHINE).setValue(FACING, Direction.EAST);
                }
                if (westMachine) {
                    return base.setValue(SHAPE, CableShape.MACHINE).setValue(FACING, Direction.WEST);
                }
                return base.setValue(SHAPE, CableShape.STRAIGHT).setValue(FACING, Direction.EAST);
            }
            if (north && south) {
                if (northMachine) {
                    return base.setValue(SHAPE, CableShape.MACHINE).setValue(FACING, Direction.NORTH);
                }
                if (southMachine) {
                    return base.setValue(SHAPE, CableShape.MACHINE).setValue(FACING, Direction.SOUTH);
                }
                return base.setValue(SHAPE, CableShape.STRAIGHT).setValue(FACING, Direction.NORTH);
            }
        }

        if (count == 1) {
            if (east) {
                if (eastMachine) return base.setValue(SHAPE, CableShape.MACHINE).setValue(FACING, Direction.EAST);
                return base.setValue(SHAPE, CableShape.STRAIGHT).setValue(FACING, Direction.EAST);
            }
            if (west) {
                if (westMachine) return base.setValue(SHAPE, CableShape.MACHINE).setValue(FACING, Direction.WEST);
                return base.setValue(SHAPE, CableShape.STRAIGHT).setValue(FACING, Direction.EAST);
            }
            if (north) {
                if (northMachine) return base.setValue(SHAPE, CableShape.MACHINE).setValue(FACING, Direction.NORTH);
                return base.setValue(SHAPE, CableShape.STRAIGHT).setValue(FACING, Direction.NORTH);
            }
            if (south) {
                if (southMachine) return base.setValue(SHAPE, CableShape.MACHINE).setValue(FACING, Direction.SOUTH);
                return base.setValue(SHAPE, CableShape.STRAIGHT).setValue(FACING, Direction.NORTH);
            }
        }

        // count == 0
        Direction f = base.hasProperty(FACING) ? base.getValue(FACING) : Direction.EAST;
        if (f.getAxis() == Direction.Axis.Y) f = Direction.EAST;
        return base.setValue(SHAPE, CableShape.STRAIGHT).setValue(FACING, f);
    }

    private static boolean isCable(BlockGetter level, BlockPos nPos) {
        return level.getBlockState(nPos).getBlock() instanceof CopperCableBlock;
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
                || nBlock instanceof BellowsEjectorBlock) {
            return true;
        }
        return level.getBlockEntity(nPos) instanceof EnergyInspectable;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(() -> new CopperCableBlock(Properties.of().strength(0.8F).noOcclusion(), insulated));
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BOX_SHAPE;
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
