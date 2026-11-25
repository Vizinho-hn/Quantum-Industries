package br.com.quantumindustries.thermal;

import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record ThermalSnapshot(int temperatureC, int ambientC, long storedThermalEnergyTu, int heatCapacityTuPerC,
                              long heatingRateTu, long coolingRateTu, long netHeatFlowTu,
                              int targetTemperatureC, int maxTemperatureC, List<ThermalFlowEdge> edges) {
    public ThermalSnapshot {
        edges = List.copyOf(edges);
    }

    /** Positive flow enters the body represented by the snapshot. */
    public record ThermalFlowEdge(Direction face, BlockPos neighbor, int heatFlowTu) {
        public ThermalFlowEdge {
            Objects.requireNonNull(face);
            neighbor = neighbor.immutable();
        }
    }
}
