package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** One generalized rotor with four ports, not four independent sources. */
public final class CompactGearboxBlockEntity extends RotationalBlockEntity {
    public static final double PORT_EFFICIENCY = .98;
    public CompactGearboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COMPACT_GEARBOX_ENTITY.get(), pos, state);
    }

    public Direction.Axis excludedAxis() {
        return getBlockState().getValue(BlockStateProperties.AXIS);
    }
    @Override public Direction.Axis axle() {
        return excludedAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
    }
    @Override public boolean connectsMechanical(Direction face) { return face.getAxis() != excludedAxis(); }
    @Override public double kineticPortRatio(Direction face) {
        return face.getAxis() == axle() ? 1.0 : -1.0;
    }
    @Override public double kineticPortEfficiency(Direction face) { return PORT_EFFICIENCY; }
    @Override public boolean allowsCrank() { return false; }
    @Override public int sourceCapacity() { return 0; }
    @Override public double kineticInertia() { return 1.6; }
    @Override public double kineticDrag() { return .09; }
    @Override public double staticFrictionTorque() { return .06; }
}
