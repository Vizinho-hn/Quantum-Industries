package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.JawCrusherBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class JawCrusherBlock extends BaseEntityBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty REFINED = BooleanProperty.create("refined");
    public static final BooleanProperty NARROW = BooleanProperty.create("narrow");
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);
    public static final float FORMED_SCALE = 1.8f;
    public static final float NARROW_WIDTH_SCALE = 1.06f;
    public static final float DRIVE_ANCHOR_X = .06f;
    public static final float AUTHORED_WHEEL_Z = (8 + 3.0576f) / 16;
    private static final int PART_CHECK_INTERVAL = 100;
    private static final Map<Direction, VoxelShape> COMPACT_SHAPES = Shapes.rotateHorizontal(Shapes.or(
            Block.box(2.3, 0, 3.2, 14.95, 9.5, 15.15),
            Block.box(4.6, 0, .45, 10.6, 3.55, 3.8),
            Block.box(.9, 3.4, 3.7, 3.75, 12.25, 12.2),
            Block.box(3.5, 9.5, 3.8, 13.8, 14.6, 12.5)
    ).optimize());
    private static final Map<Direction, VoxelShape[]> FORMED_SHAPES = formedShapes(FORMED_SCALE);
    private static final Map<Direction, VoxelShape[]> NARROW_SHAPES = formedShapes(NARROW_WIDTH_SCALE);

    private static final Map<Direction, VoxelShape[]> REFINED_SHAPES = refinedShapes();
    private static final Map<Direction, VoxelShape> REFINED_COMPACT = Shapes.rotateHorizontal(Shapes.or(
            Block.box(.35, 0, .35, 5.3, 3, 15.65),
            Block.box(10.7, 0, .35, 15.65, 3, 15.65),
            Block.box(5.3, 0, .35, 10.7, 3, 5.3),
            Block.box(5.3, 0, 10.7, 10.7, 3, 15.65),
            Block.box(2, 3, 3, 15, 14, 15),
            Block.box(15, 5, 5, 16, 11, 11),
            Block.box(3, 13, 3, 13, 16, 13),
            Block.box(0, 2, 2, 3, 14, 14),
            Block.box(4, 3, 0, 12, 9, 4)
    ).optimize());

    private static Map<Direction, VoxelShape[]> refinedShapes() {
        var drive = Shapes.rotateHorizontal(Shapes.or(
                Block.box(0, 0, 0, 16, 3, 16), Block.box(0, 2, 2, 15, 15, 14)));
        var brace = Shapes.rotateHorizontal(Shapes.or(
                Block.box(0, 0, 0, 16, 3, 16), Block.box(1, 3, 1, 5, 14, 15)));
        var chamber = Shapes.rotateHorizontal(Shapes.or(
                Block.box(0, 0, 0, 16, 13, 16), Block.box(3, 13, 3, 13, 16, 13)));
        var discharge = Shapes.rotateHorizontal(Shapes.or(
                Block.box(0, 0, 0, 16, 3, 16), Block.box(3, 3, 0, 13, 10, 16)));
        var result = new EnumMap<Direction, VoxelShape[]>(Direction.class);
        drive.forEach((facing, shape) -> result.put(facing,
                new VoxelShape[]{shape, brace.get(facing), chamber.get(facing), discharge.get(facing)}));
        return result;
    }

    private static Map<Direction, VoxelShape[]> formedShapes(float widthScale) {
        VoxelShape north = Shapes.or(
                shellBox(widthScale, 2.3, 0, 3.2, 14.95, 9.5, 15.15),
                shellBox(widthScale, 4.6, 0, .45, 10.6, 3.55, 3.8),
                shellBox(widthScale, 3.5, 9.5, 3.8, 13.8, 14.6, 12.5),
                Shapes.box(0, .025, .03, .46, .98, .97)
        ).optimize();
        Map<Direction, VoxelShape[]> result = new EnumMap<>(Direction.class);
        Shapes.rotateHorizontal(north).forEach((facing, shape) -> {
            VoxelShape[] cells = new VoxelShape[8];
            for (int part = 0; part < cells.length; part++) {
                BlockPos offset = partOffset(facing, part);
                cells[part] = Shapes.join(shape.move(-offset.getX(), -offset.getY(), -offset.getZ()),
                        Shapes.block(), BooleanOp.AND);
            }
            result.put(facing, cells);
        });
        return result;
    }

    private static VoxelShape shellBox(float widthScale, double x0, double y0, double z0, double x1, double y1, double z1) {
        double scale = FORMED_SCALE / 16.0;
        double x = DRIVE_ANCHOR_X * (1 - widthScale), z = .5 - AUTHORED_WHEEL_Z * FORMED_SCALE;
        return Shapes.box(x + x0 * widthScale / 16, y0 * scale, z + z0 * scale,
                x + x1 * widthScale / 16, y1 * scale, z + z1 * scale);
    }

    public static Direction driveFace(BlockState state) {
        Direction front = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return state.getValue(REFINED) && !state.getValue(FORMED)
                ? front.getClockWise() : front.getCounterClockWise();
    }

    public static BlockPos partOffset(Direction facing, int part) {
        return BlockPos.ZERO.relative(facing, part & 1)
                .relative(facing.getClockWise(), (part >> 1) & 1).above(part >> 2);
    }

    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        return pos.subtract(partOffset(state.getValue(BlockStateProperties.HORIZONTAL_FACING), state.getValue(PART)));
    }

    public static JawCrusherBlockEntity machineAt(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos root = controllerPos(pos, state);
        if (level instanceof ServerLevel server && !server.hasChunkAt(root)) return null;
        if (!pos.equals(root) && level.getBlockState(root) != state.setValue(PART, 0)) return null;
        return level.getBlockEntity(root) instanceof JawCrusherBlockEntity crusher ? crusher : null;
    }

    private static BlockPos at(BlockPos root, BlockState state, int part) {
        return root.offset(partOffset(state.getValue(BlockStateProperties.HORIZONTAL_FACING), part));
    }

    public static boolean isInputPort(BlockState state, Direction face) {
        if (!state.getValue(REFINED)) return true;
        return face == Direction.UP && state.getValue(PART) == (state.getValue(FORMED) ? 2 : 0);
    }

    public static boolean isOutputPort(BlockState state, Direction face) {
        if (!state.getValue(REFINED)) return true;
        return (face == null || face == Direction.DOWN || face == state.getValue(BlockStateProperties.HORIZONTAL_FACING))
                && state.getValue(PART) == (state.getValue(FORMED) ? 3 : 0);
    }

    private static boolean usesPart(BlockState state, int part) {
        if (state.getValue(REFINED)) return part < 4;
        return !state.getValue(NARROW) || (part & 2) == 0;
    }

    public static boolean assemblyIntact(ServerLevel level, BlockPos root, BlockState state) {
        if (!state.getValue(FORMED)) return true;
        for (int part = 1; part < 8; part++) {
            if (!usesPart(state, part)) continue;
            BlockPos pos = at(root, state, part);
            if (level.hasChunkAt(pos) && level.getBlockState(pos) != state.setValue(PART, part)) return false;
        }
        return true;
    }

    private static boolean canForm(Level level, BlockPos root, BlockState state, Player player) {
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        for (int part = 1; part < 8; part++) {
            if (!usesPart(state, part)) continue;
            BlockPos pos = at(root, state, part);
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()
                    || !level.isUnobstructed(state.setValue(PART, part), pos, collision)) return false;
        }
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (state.getValue(REFINED)) {
            if (state.getValue(FORMED) && !usesPart(state, state.getValue(PART))) return Shapes.empty();
            return state.getValue(FORMED) ? REFINED_SHAPES.get(facing)[state.getValue(PART)] : REFINED_COMPACT.get(facing);
        }
        return state.getValue(FORMED) ? (state.getValue(NARROW) ? NARROW_SHAPES : FORMED_SHAPES).get(facing)[state.getValue(PART)] : COMPACT_SHAPES.get(facing);
    }

    public JawCrusherBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(FORMED, false).setValue(REFINED, false).setValue(NARROW, false).setValue(PART, 0));
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel server)) return;
        Player player = placer instanceof Player p ? p : null;
        if (state.getValue(FORMED)) {
            if (canForm(server, pos, state, player)) {
                for (int part = 1; part < 8; part++) {
                    if (!usesPart(state, part)) continue;
                    BlockPos target = at(pos, state, part);
                    server.setBlockAndUpdate(target, state.setValue(PART, part));
                    server.scheduleTick(target, this, PART_CHECK_INTERVAL + part);
                }
            } else {
                server.setBlockAndUpdate(pos, state.setValue(FORMED, false));
            }
        }
        if (player != null && server.getBlockEntity(pos) instanceof JawCrusherBlockEntity machine) machine.bindAutomation(player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        if (!state.getValue(FORMED)) return;
        BlockPos root = controllerPos(pos, state);
        BlockState controller = state.setValue(PART, 0);
        if (state.getValue(PART) != 0) {
            if (level.hasChunkAt(root) && level.getBlockState(root) == controller) level.removeBlock(root, false);
            return;
        }
        for (int part = 1; part < 8; part++) {
            if (!usesPart(state, part)) continue;
            BlockPos target = at(root, state, part);
            if (level.hasChunkAt(target) && level.getBlockState(target) == state.setValue(PART, part))
                level.removeBlock(target, false);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FORMED) || state.getValue(PART) == 0) return;
        BlockPos root = controllerPos(pos, state);
        if (level.hasChunkAt(root) && level.getBlockState(root) != state.setValue(PART, 0)) {
            level.removeBlock(pos, false);
        } else {
            level.scheduleTick(pos, this, PART_CHECK_INTERVAL);
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(JawCrusherBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new JawCrusherBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(PART) != 0 ? null : createTickerHelper(type, ModBlocks.JAW_CRUSHER_ENTITY.get(),
                (l, p, s, entity) -> entity.tick());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, FORMED, REFINED, NARROW, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite()).setValue(REFINED, true);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.HAND_CRANK.get())) {
            if (!state.getValue(REFINED) || !br.com.quantumindustries.material.CrushingCatalog.isValidInput(stack)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (level instanceof ServerLevel server) {
                JawCrusherBlockEntity be = machineAt(server, pos, state);
                if (be == null || !be.bindAutomation(player)) return InteractionResult.SUCCESS;
                be.insert(player.getAbilities().instabuild ? stack.copy() : stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer op)) {
            return InteractionResult.SUCCESS;
        }
        if (!WorldResearchData.get(server).knows(op, "mineral_beneficiation")) {
            op.sendOverlayMessage(Component.translatable("message.quantumindustries.jaw_crusher.research"));
            return InteractionResult.SUCCESS;
        }
        JawCrusherBlockEntity be = machineAt(server, pos, state);
        if (be == null) {
            return InteractionResult.SUCCESS;
        }
        if (!be.bindAutomation(player) || !be.manualStroke()) op.sendOverlayMessage(be.inspectDetail());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockPos root = controllerPos(pos, state);
        if (level instanceof ServerLevel server) {
            JawCrusherBlockEntity be = machineAt(server, pos, state);
            if (be == null) return InteractionResult.SUCCESS;
            boolean authorized = be.bindAutomation(player);
            if (state.getValue(REFINED) && player.isShiftKeyDown() && authorized) {
                ItemStack taken = be.take(true);
                if (!taken.isEmpty() && !player.getInventory().add(taken)) player.drop(taken, false);
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer op) op.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, owner) -> new br.com.quantumindustries.menu.MachineProcessingMenu(id, inventory, root, true),
                    Component.translatable("block.quantumindustries.jaw_crusher")), root);
        }
        return InteractionResult.SUCCESS;
    }
}
