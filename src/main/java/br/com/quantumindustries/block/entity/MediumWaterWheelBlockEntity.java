package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class MediumWaterWheelBlockEntity extends RimWaterWheelBlockEntity {
    public static final int CAPACITY = 36;
    public MediumWaterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MEDIUM_WATER_WHEEL_ENTITY.get(), pos, state, 1, CAPACITY, 14, 1.4, 8);
    }
}
