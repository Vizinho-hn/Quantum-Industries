package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class IronFlywheelBlockEntity extends RotationalBlockEntity {
    public IronFlywheelBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.IRON_FLYWHEEL_ENTITY.get(), pos, state); }
    @Override public Direction.Axis axle() { return getBlockState().getValue(BlockStateProperties.AXIS); }
    @Override public boolean allowsCrank() { return false; }
    @Override public int sourceCapacity() { return 0; }
    @Override public double kineticInertia() { return 18; }
    @Override public double kineticDrag() { return .06; }
    @Override public double staticFrictionTorque() { return .05; }
}
