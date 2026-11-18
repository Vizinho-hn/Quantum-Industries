package br.com.quantumindustries.energy;

import br.com.quantumindustries.block.ManualDynamoBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Electrical faces in world coordinates, shared by rendering and server transfer. */
public final class EnergyPorts {
    private EnergyPorts() {}

    public static boolean connects(BlockState state, Direction face) {
        if (state.getBlock() instanceof ManualDynamoBlock) {
            return face == state.getValue(BlockStateProperties.HORIZONTAL_FACING).getCounterClockWise();
        }
        return true;
    }
}
