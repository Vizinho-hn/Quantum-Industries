package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.PeristalticPumpBlockEntity;
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

public final class PeristalticPumpBlock extends BaseEntityBlock {
    public PeristalticPumpBlock(Properties properties) {
        super(properties);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(PeristalticPumpBlock::new);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.HORIZONTAL_FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s){return new PeristalticPumpBlockEntity(pos,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState s,BlockEntityType<T> type){
        return level.isClientSide()?null:createTickerHelper(type,ModBlocks.PERISTALTIC_PUMP_ENTITY.get(),(l,p,st,be)->be.tick());
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit){
        if(!stack.is(ModItems.HAND_CRANK.get()))return InteractionResult.TRY_WITH_EMPTY_HAND;
        if(level instanceof ServerLevel server&&player instanceof ServerPlayer op
                &&server.getBlockEntity(pos) instanceof PeristalticPumpBlockEntity be){
            if(!WorldResearchData.get(server).knows(op,"peristaltic_mechanics"))
                op.sendOverlayMessage(Component.translatable("message.quantumindustries.pump.research"));
            else if(!be.pump(false))op.sendOverlayMessage(Component.translatable("message.quantumindustries.pump.blocked"));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer op&&level.getBlockEntity(pos) instanceof PeristalticPumpBlockEntity be)
            op.sendOverlayMessage(be.inspectDetail());
        return InteractionResult.SUCCESS;
    }
}
