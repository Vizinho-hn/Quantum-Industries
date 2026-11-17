package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.BellowsAlloyKilnBlockEntity;
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
import net.minecraft.world.item.Items;
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

public final class BellowsAlloyKilnBlock extends BaseEntityBlock {
    public BellowsAlloyKilnBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(BellowsAlloyKilnBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BellowsAlloyKilnBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.BELLOWS_ALLOY_KILN_ENTITY.get(),
                (l, p, s, be) -> be.tick());
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        boolean isFlux = ModItems.CHEMICAL_PRODUCTS.containsKey("fluorite")
                && stack.is(ModItems.CHEMICAL_PRODUCTS.get("fluorite").get());
        boolean isFuel = stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || isFlux;
        boolean isBellows = stack.is(ModItems.MANUAL_BELLOWS.get());
        boolean isIngredient = AlloyCatalog.isValidIngredient(stack, 1);

        if (!isFuel && !isBellows && !isIngredient) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer op)) return InteractionResult.SUCCESS;

        if (!WorldResearchData.get(server).knows(op, "alloy_metallurgy")) {
            op.sendOverlayMessage(Component.translatable("block.quantumindustries.bellows_alloy_kiln.research"));
            return InteractionResult.SUCCESS;
        }

        if (server.getBlockEntity(pos) instanceof BellowsAlloyKilnBlockEntity kiln) {
            if (isBellows) {
                if (kiln.puff()) {
                    server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.35f, 1.1f);
                    op.sendOverlayMessage(kiln.status());
                }
            } else if (kiln.insert(stack)) {
                server.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.4f, 1.0f);
                op.sendOverlayMessage(kiln.status());
            } else {
                op.sendOverlayMessage(Component.translatable("block.quantumindustries.bellows_alloy_kiln.busy"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof BellowsAlloyKilnBlockEntity kiln) {
            ItemStack taken = kiln.take(player.isShiftKeyDown());
            if (!taken.isEmpty()) {
                player.getInventory().placeItemBackInInventory(taken);
                server.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.35f, 1.1f);
            } else if (player instanceof ServerPlayer op) {
                op.sendOverlayMessage(kiln.status());
            }
        }
        return InteractionResult.SUCCESS;
    }
}
