package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.MechanicalShaftBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class MechanicalShaftBlock extends BaseEntityBlock {
    public MechanicalShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(MechanicalShaftBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.AXIS); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(BlockStateProperties.AXIS, context.getClickedFace().getAxis()); }
    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.AXIS)) {
            case X -> Block.box(0,4,4,16,12,12);
            case Y -> Block.box(4,0,4,12,16,12);
            case Z -> Block.box(4,4,0,12,12,16);
        };
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MechanicalShaftBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.MECHANICAL_SHAFT_ENTITY.get(), (l,p,s,b) -> b.tick());
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.HAND_CRANK.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MechanicalShaftBlockEntity shaft) {
            shaft.installCrank(player, stack);
            server.sendOverlayMessage(Component.translatable("gui.quantumindustries.mechanical.help"));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof MechanicalShaftBlockEntity shaft) {
            var crank = player.isShiftKeyDown() ? shaft.detachCrank(player) : ItemStack.EMPTY;
            if (!crank.isEmpty()) player.getInventory().placeItemBackInInventory(crank);
            else shaft.turn(player);
            var network = MechanicalNetwork.state((ServerLevel) level, pos);
            server.sendOverlayMessage(Component.translatable("gui.quantumindustries.mechanical.network", network.capacity(), network.load()));
        }
        return InteractionResult.SUCCESS;
    }
}
