package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.HydraulicTorque;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;

public final class PrimitiveWaterWheelBlockEntity extends RotationalBlockEntity {
    public static final int CAPACITY = 18;
    private double lastHydraulicTorque;

    public PrimitiveWaterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PRIMITIVE_WATER_WHEEL_ENTITY.get(), pos, state);
    }

    @Override public Direction.Axis axle() {
        return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis();
    }

    @Override public boolean allowsCrank() { return false; }

    @Override protected boolean usesProceduralRotor() { return true; }

    public double hydraulicTorque() {
        if (level instanceof ServerLevel server) {
            lastHydraulicTorque = sampleHydraulicTorque(server).torque();
        }
        return lastHydraulicTorque;
    }

    private HydraulicTorque.Result sampleHydraulicTorque(ServerLevel server) {
        Direction.Axis axis = axle();
        Direction tangent = axis == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
        List<HydraulicTorque.Sample> samples = new ArrayList<>(4);

        for (var pos : List.of(worldPosition.above(), worldPosition.below(),
                worldPosition.relative(tangent), worldPosition.relative(tangent.getOpposite()))) {
            if (!server.hasChunkAt(pos)) continue;
            // Fluid#getFlow inspects adjacent cells. Verify their chunks before calling it.
            boolean loaded = true;
            for (Direction side : Direction.values()) {
                if (!server.hasChunkAt(pos.relative(side))) { loaded = false; break; }
            }
            if (!loaded) continue;

            var fluid = server.getFluidState(pos);
            if (!fluid.is(FluidTags.WATER) || fluid.isSource()) continue;

            boolean falling = fluid.hasProperty(FlowingFluid.FALLING) && fluid.getValue(FlowingFluid.FALLING);
            var flow = fluid.getFlow(server, pos);
            double fx = flow.x, fy = flow.y, fz = flow.z;
            if (falling) fy = Math.min(fy, -1.0);

            int rx = pos.getX() - worldPosition.getX();
            int ry = pos.getY() - worldPosition.getY();
            int rz = pos.getZ() - worldPosition.getZ();

            // A falling water block placed directly under/above the wheel often has zero horizontal
            // velocity in getFlow(); infer the feed vector from adjacent water cells so spin direction is preserved.
            if (falling && ry != 0) {
                double tanFlow = axis == Direction.Axis.X ? fz : fx;
                if (Math.abs(tanFlow) <= 0.01) {
                    boolean hasTanWater = server.getFluidState(pos.relative(tangent)).is(FluidTags.WATER);
                    boolean hasOppWater = server.getFluidState(pos.relative(tangent.getOpposite())).is(FluidTags.WATER);
                    if (hasTanWater && !hasOppWater) {
                        var dir = tangent.getOpposite();
                        if (axis == Direction.Axis.X) fz = dir.getStepZ();
                        else fx = dir.getStepX();
                    } else if (hasOppWater && !hasTanWater) {
                        if (axis == Direction.Axis.X) fz = tangent.getStepZ();
                        else fx = tangent.getStepX();
                    }
                }
            }

            double tangential = axis == Direction.Axis.X ? fz : fx;
            if (!falling && Math.abs(tangential) <= 0.01 && fy >= -0.01) continue;

            samples.add(new HydraulicTorque.Sample(rx, ry, rz, fx, fy, fz));
        }
        return HydraulicTorque.evaluate(axis, samples);
    }

    @Override public int sourceCapacity() {
        if (!(level instanceof ServerLevel server)) return 0;
        var sample = sampleHydraulicTorque(server);
        lastHydraulicTorque = sample.torque();
        return sample.active() ? CAPACITY : 0;
    }

    @Override public int sourceDirection() {
        if (!(level instanceof ServerLevel server)) return 1;
        var sample = sampleHydraulicTorque(server);
        lastHydraulicTorque = sample.torque();
        return sample.direction();
    }

    public void tick() {
        if (level instanceof ServerLevel server) {
            lastHydraulicTorque = sampleHydraulicTorque(server).torque();
            var state = MechanicalNetwork.state(server, worldPosition);
            setMotion(state.powered(), state.spinDirection());
        }
    }
}
