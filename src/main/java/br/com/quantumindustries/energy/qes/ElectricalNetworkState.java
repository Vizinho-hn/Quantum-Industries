package br.com.quantumindustries.energy.qes;

import java.util.List;

public record ElectricalNetworkState(NetworkStatus status, List<ElectricalNodeState> nodes,
                                     List<ElectricalNodeState> edges, List<ElectricalNodeState> sources,
                                     List<ElectricalNodeState> loads, double extractedFePerTick,
                                     double deliveredFePerTick, double lossFePerTick) {
    public enum NetworkStatus {
        IDLE, ACTIVE, CURRENT_LIMITED, UNDERVOLTAGE, SHORT_CIRCUIT_LIMITED, OPEN_CIRCUIT, SOLVER_BUDGET_EXCEEDED
    }

    public ElectricalNetworkState {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        sources = List.copyOf(sources);
        loads = List.copyOf(loads);
    }
}
