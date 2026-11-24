package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-terminal, zero-buffer insulated circuit breaker. */
public final class IndustrialCircuitSwitchBlock extends CopperCableBlock {
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    public static final BooleanProperty REDSTONE_CONTROLLED = BooleanProperty.create("redstone_controlled");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public IndustrialCircuitSwitchBlock(Properties properties) {
        super(properties, true);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(CLOSED, false).setValue(REDSTONE_CONTROLLED, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(IndustrialCircuitSwitchBlock::new);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.HORIZONTAL_FACING, CLOSED, REDSTONE_CONTROLLED);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    public static boolean permits(BlockState state, Direction side) {
        return state.getValue(CLOSED)
                && state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() == side.getAxis();
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer operator)) return InteractionResult.SUCCESS;
        if (!operator.mayBuild() || !server.mayInteract(operator, pos)) return InteractionResult.FAIL;
        BlockState next;
        if (operator.isShiftKeyDown()) {
            boolean redstone = !state.getValue(REDSTONE_CONTROLLED);
            next = state.setValue(REDSTONE_CONTROLLED, redstone);
            if (redstone) next = next.setValue(CLOSED, server.hasNeighborSignal(pos));
        } else if (!state.getValue(REDSTONE_CONTROLLED)) {
            next = state.cycle(CLOSED);
        } else {
            next = state.setValue(CLOSED, server.hasNeighborSignal(pos));
        }
        if (next != state) server.setBlock(pos, next, Block.UPDATE_ALL);
        operator.sendOverlayMessage(Component.translatable("message.quantumindustries.industrial_circuit_switch.status",
                Component.translatable(next.getValue(CLOSED) ? "message.quantumindustries.industrial_circuit_switch.closed"
                        : "message.quantumindustries.industrial_circuit_switch.open"),
                Component.translatable(next.getValue(REDSTONE_CONTROLLED) ? "message.quantumindustries.industrial_circuit_switch.redstone"
                        : "message.quantumindustries.industrial_circuit_switch.manual")));
        return InteractionResult.SUCCESS;
    }

    public static void refreshRedstone(ServerLevel server, BlockPos pos, BlockState state) {
        if (state.getValue(REDSTONE_CONTROLLED)) {
            boolean signal = server.hasNeighborSignal(pos);
            if (signal != state.getValue(CLOSED)) server.setBlock(pos, state.setValue(CLOSED, signal), Block.UPDATE_ALL);
        }
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source,
            Orientation orientation, boolean movedByPiston) {
        if (level instanceof ServerLevel server) refreshRedstone(server, pos, state);
        super.neighborChanged(state, level, pos, source, orientation, movedByPiston);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(ModItems.INDUSTRIAL_CIRCUIT_SWITCH.get());
    }
}
