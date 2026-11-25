package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.ElectricalNetworkAnalyzerBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.menu.ElectricalNetworkAnalyzerMenu;
import br.com.quantumindustries.research.WorldResearchData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public final class ElectricalNetworkAnalyzerBlock extends BaseEntityBlock {
    public static final String RESEARCH="load_protection";
    public ElectricalNetworkAnalyzerBlock(Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(ElectricalNetworkAnalyzerBlock::new);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(BlockStateProperties.FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        return defaultBlockState().setValue(BlockStateProperties.FACING,context.getClickedFace());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ElectricalNetworkAnalyzerBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide()?null:createTickerHelper(type,ModBlocks.ELECTRICAL_NETWORK_ANALYZER_ENTITY.get(),(l,p,s,b)->b.tick());
    }
    public static boolean allowed(ServerLevel server,Player player,BlockPos pos){
        return player.level()==server&&player.distanceToSqr(pos.getCenter())<=64&&server.mayInteract(player,pos)
                &&WorldResearchData.get(server).knows(player,RESEARCH);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level instanceof ServerLevel server&&player instanceof ServerPlayer op){
            if(!allowed(server,op,pos)){
                op.sendOverlayMessage(Component.translatable("message.quantumindustries.network_analyzer.locked"));
                return InteractionResult.SUCCESS;
            }
            op.openMenu(new SimpleMenuProvider((id,inv,p)->new ElectricalNetworkAnalyzerMenu(id,inv,pos),
                    Component.translatable("block.quantumindustries.electrical_network_analyzer")),pos);
        }
        return InteractionResult.SUCCESS;
    }
}
