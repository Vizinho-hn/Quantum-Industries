package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class PrimitiveWaterWheelBlockEntity extends RotationalBlockEntity {
    public static final int CAPACITY = 18;
    public PrimitiveWaterWheelBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.PRIMITIVE_WATER_WHEEL_ENTITY.get(), pos, state); }
    @Override public Direction.Axis axle() { return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis(); }
    @Override public boolean allowsCrank() { return false; }
    @Override public int sourceCapacity() {
        if (!(level instanceof ServerLevel server)) return 0;
        Direction tangent = axle() == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
        for (var pos : java.util.List.of(worldPosition.above(), worldPosition.below(), worldPosition.relative(tangent), worldPosition.relative(tangent.getOpposite()))) {
            if (!server.hasChunkAt(pos)) continue;
            // Fluid#getFlow inspects adjacent cells. Verify their chunks before calling it.
            boolean loaded = true;
            for (Direction side : Direction.values()) if (!server.hasChunkAt(pos.relative(side))) { loaded = false; break; }
            if (!loaded) continue;
            var fluid = server.getFluidState(pos);
            if (!fluid.is(FluidTags.WATER) || fluid.isSource()) continue;
            if (fluid.hasProperty(net.minecraft.world.level.material.FlowingFluid.FALLING)
                    && fluid.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING)) return CAPACITY;
            var flow = fluid.getFlow(server, pos);
            double tangential = axle() == Direction.Axis.X ? flow.z : flow.x;
            if (Math.abs(tangential) > .01 || flow.y < -.01) return CAPACITY;
        }
        return 0;
    }
    public void tick() {
        if (level instanceof ServerLevel server) setRotating(MechanicalNetwork.state(server, worldPosition).powered());
    }
}
