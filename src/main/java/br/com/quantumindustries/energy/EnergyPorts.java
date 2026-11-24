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
        if (state.getBlock() instanceof br.com.quantumindustries.block.IndustrialConversionBlock conversion) {
            return switch (conversion.kind()) {
                case GENERATOR, MOTOR -> face == state.getValue(BlockStateProperties.HORIZONTAL_FACING).getCounterClockWise();
                case THERMOELECTRIC -> face == state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                default -> false;
            };
        }
        return true;
    }
}
