package br.com.quantumindustries.block;

import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Stores at most 15 pulse units. The demonstration output is a vanilla redstone signal. */
public final class ManualDynamoBlock extends Block {
    private static final IntegerProperty CHARGE = BlockStateProperties.POWER;
    private static final BooleanProperty CRANKED = BlockStateProperties.POWERED;
    private static final int INTERVAL = 8;

    public ManualDynamoBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0).setValue(CRANKED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE, CRANKED);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer operator)) {
            return InteractionResult.SUCCESS;
        }
        if (!WorldResearchData.get(server).knows(player, "electromagnetic_induction")) {
            operator.sendOverlayMessage(Component.literal("Pré-requisito: Indução Eletromagnética."));
            return InteractionResult.SUCCESS;
        }
        int charge = state.getValue(CHARGE);
        if (!state.getValue(CRANKED) && charge < 15) {
            charge = Math.min(15, charge + 4);
            level.setBlock(pos, state.setValue(CHARGE, charge).setValue(CRANKED, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, INTERVAL);
        }
        operator.sendOverlayMessage(Component.literal("Carga: " + charge + "/15 · saída de redstone"));
        return InteractionResult.SUCCESS;
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int charge = Math.max(0, state.getValue(CHARGE) - 1);
        level.setBlock(pos, state.setValue(CHARGE, charge).setValue(CRANKED, false), Block.UPDATE_ALL);
        if (charge > 0) level.scheduleTick(pos, this, INTERVAL);
    }

    @Override protected boolean isSignalSource(BlockState state) { return true; }

    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(CHARGE);
    }

    @Override protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(CHARGE);
    }
}
