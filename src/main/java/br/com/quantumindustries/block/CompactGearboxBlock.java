package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.CompactGearboxBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** AXIS is the excluded axis; the remaining two axes carry a bevel-gear transmission. */
public final class CompactGearboxBlock extends AxialTransmissionBlock {
    public CompactGearboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(CompactGearboxBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CompactGearboxBlockEntity(pos, state); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var excluded = state.getValue(BlockStateProperties.AXIS);
        VoxelShape shape = Block.box(2, 2, 2, 14, 14, 14);
        if (excluded != Direction.Axis.X) shape = Shapes.or(shape, Block.box(0, 4.5, 4.5, 16, 11.5, 11.5));
        if (excluded != Direction.Axis.Y) shape = Shapes.or(shape, Block.box(4.5, 0, 4.5, 11.5, 16, 11.5));
        if (excluded != Direction.Axis.Z) shape = Shapes.or(shape, Block.box(4.5, 4.5, 0, 11.5, 11.5, 16));
        return shape;
    }
}
