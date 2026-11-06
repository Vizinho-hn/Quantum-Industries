package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.PiezoSwitchBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.redstone.Orientation;

public final class PiezoSwitchBlock extends BaseEntityBlock {
    private static final VoxelShape PLATE = Block.box(1, 0, 1, 15, 2, 15);
    public PiezoSwitchBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(PiezoSwitchBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return PLATE;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PiezoSwitchBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.PIEZO_SWITCH_ENTITY.get(),
                (l, p, s, be) -> be.tick());
    }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server
                && server.getBlockEntity(pos) instanceof PiezoSwitchBlockEntity be) be.impact(entity);
        super.stepOn(level, pos, state, entity);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source,
            Orientation orientation, boolean movedByPiston) {
        if (level instanceof ServerLevel server && (source == Blocks.PISTON || source == Blocks.STICKY_PISTON)
                && movedByPiston && server.getBlockEntity(pos) instanceof PiezoSwitchBlockEntity be) {
            be.pistonImpact();
        }
        super.neighborChanged(state, level, pos, source, orientation, movedByPiston);
    }
}
