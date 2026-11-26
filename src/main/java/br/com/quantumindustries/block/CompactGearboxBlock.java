package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.CompactGearboxBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** AXIS is the excluded axis; the remaining two axes carry a bevel-gear transmission. */
public final class CompactGearboxBlock extends AxialTransmissionBlock {
    public CompactGearboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(CompactGearboxBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CompactGearboxBlockEntity(pos, state); }
}
