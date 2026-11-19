package br.com.quantumindustries.logistics;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public interface ConfigurableConduit {
    ConnectionModes connectionModes();
    void connectionChanged();

    default ConnectionMode connectionMode(Direction face) { return connectionModes().get(face); }

    static ConnectionMode modeAt(BlockGetter level, BlockPos pos, Direction face) {
        return level.getBlockEntity(pos) instanceof ConfigurableConduit conduit
                ? conduit.connectionMode(face) : ConnectionMode.AUTO;
    }
}
