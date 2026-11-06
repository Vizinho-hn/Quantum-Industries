package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.FluidPipeBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class FluidPipeBlock extends BaseEntityBlock {
    private final boolean glass;
    public FluidPipeBlock(Properties properties, boolean glass) { super(properties);this.glass=glass; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){
        return MapCodec.unit(()->new FluidPipeBlock(Properties.of().strength(1.0f).noOcclusion(),glass));
    }
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return Block.box(4,4,0,12,12,16);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FluidPipeBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide()?null:createTickerHelper(type,ModBlocks.FLUID_PIPE_ENTITY.get(),(l,p,s,be)->be.tick());
    }
}
