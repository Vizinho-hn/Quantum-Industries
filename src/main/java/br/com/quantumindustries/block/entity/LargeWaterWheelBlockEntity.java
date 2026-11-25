package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class LargeWaterWheelBlockEntity extends RimWaterWheelBlockEntity {
    public static final int CAPACITY = 54;
    @Override public int mechanicalPortSize(Direction face) { return 6; }

    public LargeWaterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LARGE_WATER_WHEEL_ENTITY.get(), pos, state, 2, CAPACITY, 32, 2.4, 12);
    }
}
