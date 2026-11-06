package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.GalvanicAccumulatorBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.BlockHitResult;

public final class GalvanicAccumulatorBlock extends BaseEntityBlock {
    public GalvanicAccumulatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(GalvanicAccumulatorBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GalvanicAccumulatorBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.GALVANIC_ACCUMULATOR_ENTITY.get(), (l, p, s, b) -> b.tick());
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof GalvanicAccumulatorBlockEntity accumulator) {
            int energy = accumulator.energy();
            int cap = GalvanicAccumulatorBlockEntity.CAPACITY;
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "block.quantumindustries.galvanic_accumulator.status", energy, cap, energy * 100 / cap));
        }
        return InteractionResult.SUCCESS;
    }
}
