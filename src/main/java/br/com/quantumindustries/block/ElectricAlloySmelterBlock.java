package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.material.AlloyCatalog;
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

public final class ElectricAlloySmelterBlock extends BaseEntityBlock {
    public ElectricAlloySmelterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(ElectricAlloySmelterBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricAlloySmelterBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.ELECTRIC_ALLOY_SMELTER_ENTITY.get(),
                (l, p, s, be) -> be.tick());
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!AlloyCatalog.isValidIngredient(stack, 2)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer op)
                || !(server.getBlockEntity(pos) instanceof ElectricAlloySmelterBlockEntity smelter)) {
            return InteractionResult.SUCCESS;
        }
        if (!WorldResearchData.get(server).knows(op, "electric_alloy_smelting")) {
            op.sendOverlayMessage(Component.translatable("block.quantumindustries.electric_alloy_smelter.research"));
            return InteractionResult.SUCCESS;
        }
        if (smelter.insertIngredient(stack)) {
            server.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4f, 1.15f);
            op.sendOverlayMessage(smelter.inspectDetail());
        } else {
            op.sendOverlayMessage(Component.translatable("block.quantumindustries.electric_alloy_smelter.busy"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer op)
                || !(server.getBlockEntity(pos) instanceof ElectricAlloySmelterBlockEntity smelter)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack extracted = smelter.extractResult(player.isShiftKeyDown());
        if (!extracted.isEmpty()) {
            op.getInventory().placeItemBackInInventory(extracted);
            server.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.35f, 1.1f);
            return InteractionResult.SUCCESS;
        }
        op.sendOverlayMessage(smelter.inspectDetail());
        return InteractionResult.SUCCESS;
    }
}
