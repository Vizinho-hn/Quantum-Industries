package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.CogwheelBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SmallCogwheelBlock extends AxialTransmissionBlock {
    public SmallCogwheelBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return simpleCodec(SmallCogwheelBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CogwheelBlockEntity(pos, state); }
}
