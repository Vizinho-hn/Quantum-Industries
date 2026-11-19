package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class MechanicalShaftBlockEntity extends RotationalBlockEntity {
    public MechanicalShaftBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.MECHANICAL_SHAFT_ENTITY.get(), pos, state); }
    @Override public Direction.Axis axle() { return getBlockState().getValue(BlockStateProperties.AXIS); }
    @Override protected boolean usesProceduralRotor() { return true; }
    public void tick() {
        if (level instanceof ServerLevel server) {
            var state = MechanicalNetwork.state(server, worldPosition);
            setMotion(state.powered(), state.spinDirection());
        }
    }
}
