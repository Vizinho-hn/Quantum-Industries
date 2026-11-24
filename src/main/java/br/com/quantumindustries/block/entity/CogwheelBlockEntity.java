package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class CogwheelBlockEntity extends RotationalBlockEntity {
    public static final int SMALL_TEETH = 8, LARGE_TEETH = 16;
    public static final double MESH_EFFICIENCY = .96;
    public CogwheelBlockEntity(BlockPos pos, BlockState state) {
        super(state.is(ModBlocks.SMALL_COGWHEEL.get()) ? ModBlocks.SMALL_COGWHEEL_ENTITY.get() : ModBlocks.LARGE_COGWHEEL_ENTITY.get(), pos, state);
    }
    public int teeth() { return getBlockState().is(ModBlocks.SMALL_COGWHEEL.get()) ? SMALL_TEETH : LARGE_TEETH; }
    @Override public Direction.Axis axle() { return getBlockState().getValue(BlockStateProperties.AXIS); }
    @Override public boolean allowsCrank() { return false; }
    @Override public int sourceCapacity() { return 0; }
    @Override public double kineticInertia() { return teeth() == SMALL_TEETH ? .9 : 2.4; }
    @Override public double kineticDrag() { return teeth() == SMALL_TEETH ? .05 : .08; }
    @Override public double staticFrictionTorque() { return teeth() == SMALL_TEETH ? .04 : .05; }
    @Override public List<KineticLink> kineticLinks(ServerLevel level, BlockPos pos) {
        var links = new ArrayList<KineticLink>(super.kineticLinks(level, pos));
        for (Direction side : Direction.values()) {
            if (side.getAxis() == axle()) continue;
            BlockPos next = pos.relative(side);
            if (level.hasChunkAt(next) && level.getBlockEntity(next) instanceof CogwheelBlockEntity other && other.axle() == axle())
                links.add(new KineticLink(next, -(double) teeth() / other.teeth(), MESH_EFFICIENCY));
        }
        return List.copyOf(links);
    }
}
