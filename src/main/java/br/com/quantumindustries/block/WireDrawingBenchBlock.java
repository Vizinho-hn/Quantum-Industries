package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

public final class WireDrawingBenchBlock extends BaseEntityBlock {
    public WireDrawingBenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(WireDrawingBenchBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WireDrawingBenchBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.WIRE_DRAWING_BENCH_ENTITY.get(),
                (l, p, s, be) -> be.tick());
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.HORIZONTAL_FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.is(ModItems.COPPER_SHEET.get()) || stack.is(ModItems.GOLD_SHEET.get())
                || stack.is(ModItems.HAND_CRANK.get()))) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer op)) return InteractionResult.SUCCESS;
        if (!WorldResearchData.get(server).knows(op, "wire_drawing")) {
            op.sendOverlayMessage(Component.translatable("message.quantumindustries.wire.research"));
            return InteractionResult.SUCCESS;
        }
        if (!(server.getBlockEntity(pos) instanceof WireDrawingBenchBlockEntity be)) return InteractionResult.SUCCESS;
        if (stack.is(ModItems.HAND_CRANK.get())) {
            be.manualStroke();
        } else {
            if (!be.insert(stack)) op.sendOverlayMessage(Component.translatable("message.quantumindustries.wire.busy"));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof WireDrawingBenchBlockEntity be) {
            ItemStack result = be.take(player.isShiftKeyDown());
            if (!result.isEmpty()) player.getInventory().placeItemBackInInventory(result);
            else if (player instanceof ServerPlayer op) op.sendOverlayMessage(be.inspectDetail());
        }
        return InteractionResult.SUCCESS;
    }
}
