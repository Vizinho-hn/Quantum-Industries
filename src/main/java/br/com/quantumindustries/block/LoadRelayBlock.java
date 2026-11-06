package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.LoadRelayBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class LoadRelayBlock extends BaseEntityBlock {
    public LoadRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(LoadRelayBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LoadRelayBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.LOAD_RELAY_ENTITY.get(),
                (l,p,s,be) -> be.tick());
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.POWERED);
    }
    @Override protected boolean isSignalSource(BlockState state) { return true; }
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(BlockStateProperties.POWERED) ? 15 : 0;
    }
}
