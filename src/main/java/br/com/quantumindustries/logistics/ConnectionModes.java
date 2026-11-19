package br.com.quantumindustries.logistics;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Six independent ports. Missing or malformed legacy data keeps automatic operation. */
public final class ConnectionModes {
    private final ConnectionMode[] modes = new ConnectionMode[6];

    public ConnectionModes() { Arrays.fill(modes, ConnectionMode.AUTO); }
    public ConnectionMode get(Direction side) { return modes[side.get3DDataValue()]; }
    public void set(Direction side, ConnectionMode mode) { modes[side.get3DDataValue()] = java.util.Objects.requireNonNull(mode); }
    public void save(ValueOutput out) {
        out.store("connection_modes", Codec.INT.listOf(), Arrays.stream(modes).map(Enum::ordinal).toList());
    }
    public void load(ValueInput in) {
        Arrays.fill(modes, ConnectionMode.AUTO);
        var stored = in.read("connection_modes", Codec.INT.listOf()).orElse(java.util.List.of());
        if (stored.size() != 6) return;
        var values = ConnectionMode.values();
        for (int i = 0; i < 6; i++) {
            int value = stored.get(i);
            if (value >= 0 && value < values.length) modes[i] = values[value];
        }
    }
}
