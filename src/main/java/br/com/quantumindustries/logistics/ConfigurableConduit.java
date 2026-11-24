package br.com.quantumindustries.logistics;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public interface ConfigurableConduit {
    ConnectionModes connectionModes();
    void connectionChanged();

    default ConnectionMode connectionMode(Direction face) { return connectionModes().get(face); }

    static ConnectionMode modeAt(BlockGetter level, BlockPos pos, Direction face) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof br.com.quantumindustries.block.IndustrialCircuitSwitchBlock)
            return br.com.quantumindustries.block.IndustrialCircuitSwitchBlock.permits(state, face)
                    ? ConnectionMode.AUTO : ConnectionMode.NONE;
        return level.getBlockEntity(pos) instanceof ConfigurableConduit conduit
                ? conduit.connectionMode(face) : ConnectionMode.AUTO;
    }
}
