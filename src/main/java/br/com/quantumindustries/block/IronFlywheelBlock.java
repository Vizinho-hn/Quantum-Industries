package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.IronFlywheelBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class IronFlywheelBlock extends AxialTransmissionBlock {
    public IronFlywheelBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return simpleCodec(IronFlywheelBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new IronFlywheelBlockEntity(pos, state); }
}
