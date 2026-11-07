package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.ManualGrainMillBlockEntity;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Optional Era I grain processing. No electricity or progression-era gate.
 * Right click with wheat to insert, empty hand to turn; sneak + empty hand retrieves.
 */
public final class ManualGrainMillBlock extends BaseEntityBlock {
    public ManualGrainMillBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ManualGrainMillBlock::new);
    }

    @Override protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManualGrainMillBlockEntity(pos, state);
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.WHEAT) && !stack.is(ModItems.HAND_CRANK.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer operator)) {
            return InteractionResult.SUCCESS;
        }
        if (server.getBlockEntity(pos) instanceof ManualGrainMillBlockEntity mill) {
            boolean ok = stack.is(Items.WHEAT) ? mill.insert(stack) : mill.stroke();
            operator.sendOverlayMessage(ok ? mill.status()
                    : Component.translatable("gui.quantumindustries.grain_mill.help"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer operator
                && server.getBlockEntity(pos) instanceof ManualGrainMillBlockEntity mill) {
            if (player.isShiftKeyDown()) {
                ItemStack taken = mill.take();
                if (!taken.isEmpty()) player.getInventory().placeItemBackInInventory(taken);
            } else {
                mill.stroke();
            }
            operator.sendOverlayMessage(mill.status());
        }
        return InteractionResult.SUCCESS;
    }
}
