package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public final class MechanicalSawmillBlock extends BaseEntityBlock {
    public MechanicalSawmillBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(MechanicalSawmillBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.HORIZONTAL_FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MechanicalSawmillBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.MECHANICAL_SAWMILL_ENTITY.get(), (l,p,s,b) -> b.tick());
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer server) || !(level.getBlockEntity(pos) instanceof MechanicalSawmillBlockEntity saw)) return InteractionResult.SUCCESS;
        boolean done = stack.is(ModItems.HAND_CRANK.get()) ? saw.installCrank(player, stack) : saw.insert(player, stack);
        server.sendOverlayMessage(done ? saw.status() : Component.translatable("gui.quantumindustries.sawmill.help"));
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MechanicalSawmillBlockEntity saw) {
            ItemStack taken = saw.extract(player, player.isShiftKeyDown());
            if (taken.isEmpty() && player.isShiftKeyDown()) taken = saw.detachCrank(player);
            if (!taken.isEmpty()) player.getInventory().placeItemBackInInventory(taken);
            else saw.turn(player);
            server.sendOverlayMessage(saw.status());
        }
        return InteractionResult.SUCCESS;
    }
}
