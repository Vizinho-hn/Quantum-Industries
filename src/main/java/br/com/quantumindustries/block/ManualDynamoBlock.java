package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public final class ManualDynamoBlock extends BaseEntityBlock {
    public ManualDynamoBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(ManualDynamoBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManualDynamoBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.MANUAL_DYNAMO_ENTITY.get(), (l, p, s, b) -> b.tick());
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.HAND_CRANK.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer operator && level.getBlockEntity(pos) instanceof ManualDynamoBlockEntity dynamo) {
            boolean paid = !operator.getAbilities().instabuild;
            if (dynamo.install(paid)) {
                if (paid) stack.shrink(1);
                message(operator, "installed");
            } else message(operator, "already_installed");
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer operator)
                || !(server.getBlockEntity(pos) instanceof ManualDynamoBlockEntity dynamo)) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
            if (dynamo.installed()) {
                if (dynamo.detach()) operator.getInventory().placeItemBackInInventory(new ItemStack(ModItems.HAND_CRANK.get()));
                message(operator, "removed");
            }
        } else if (!dynamo.installed()) {
            message(operator, "missing_crank");
        } else if (!WorldResearchData.get(server).knows(player, "manual_dynamo_design")) {
            message(operator, "research");
        } else if (dynamo.energy() == ManualDynamoBlockEntity.CAPACITY) {
            message(operator, "full");
        } else if (dynamo.turn()) {
            server.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, .45f, 1.15f);
        }
        return InteractionResult.SUCCESS;
    }

    private static void message(ServerPlayer player, String key) {
        player.sendOverlayMessage(Component.translatable("block.quantumindustries.manual_dynamo." + key));
    }
}
