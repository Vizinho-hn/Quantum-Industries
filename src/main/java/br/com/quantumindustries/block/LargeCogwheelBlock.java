package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.CogwheelBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class LargeCogwheelBlock extends AxialTransmissionBlock {
    public LargeCogwheelBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return simpleCodec(LargeCogwheelBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CogwheelBlockEntity(pos, state); }
}
