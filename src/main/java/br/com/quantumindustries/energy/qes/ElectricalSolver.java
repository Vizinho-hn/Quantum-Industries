package br.com.quantumindustries.energy.qes;

import br.com.quantumindustries.energy.qes.ElectricalNetworkState.NetworkStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static br.com.quantumindustries.energy.qes.ElectricalMath.*;

/** Bounded DC nodal analysis and integer FE bridge for server-controlled receivers. */
public final class ElectricalSolver {
    public static final int MAX_NODES = 128;
    public static final int MAX_EDGES = MAX_NODES * 6;
    public static final int MAX_ITERATIONS = 16;
    private static final int MIN_SHORT_RESISTANCE_MOHM = 1;
    private static final int FE_PORT_INTERNAL_RESISTANCE_MOHM = 100;
    private static final double VOLTAGE_TOLERANCE_MV = 1e-5;
    private static final double CURRENT_TOLERANCE_MA = 1e-5;

    public enum PortMode {
        AUTO, PULL, PUSH, NONE;

        private boolean sends() { return this == AUTO || this == PUSH; }
        private boolean receives() { return this == AUTO || this == PULL; }
    }

    public record Source(int node, int voltageMv, int internalResistanceMOhm, int maxCurrentMa,
                         int availableFePerTick) {
        public Source {
            if (node < 0 || voltageMv <= 0 || internalResistanceMOhm <= 0 || maxCurrentMa < 0
                    || availableFePerTick < 0) throw new IllegalArgumentException("Invalid source");
        }

        private boolean enabled() { return maxCurrentMa > 0 && availableFePerTick > 0; }
    }

    /** A zero-ohm load represents a fault across the implicit return. */
    public record Load(int node, int resistanceMOhm, int minimumVoltageMv) {
        public Load {
            if (node < 0 || resistanceMOhm < 0 || minimumVoltageMv < 0) {
                throw new IllegalArgumentException("Invalid load");
            }
        }

        private double effectiveResistance() { return Math.max(MIN_SHORT_RESISTANCE_MOHM, resistanceMOhm); }
    }

    public record Edge(int from, int to, int resistanceMOhm, int maxCurrentMa, int availableFePerTick,
                       PortMode fromMode, PortMode toMode) {
        public Edge {
            Objects.requireNonNull(fromMode);
            Objects.requireNonNull(toMode);
            if (from < 0 || to < 0 || from == to || resistanceMOhm <= 0 || maxCurrentMa < 0
                    || availableFePerTick < 0) throw new IllegalArgumentException("Invalid edge");
        }

        private boolean forwards() { return fromMode.sends() && toMode.receives(); }
        private boolean backwards() { return toMode.sends() && fromMode.receives(); }
        private boolean enabled() {
            return maxCurrentMa > 0 && availableFePerTick > 0 && (forwards() || backwards());
        }
    }

    public record FeTransfer(int delivered, int loss, double currentMa, NetworkStatus status) {
        public int extracted() { return delivered + loss; }

        /** Integer loss accumulated through the source resistance and a prefix of line resistance. */
        public int lossThrough(int lineResistanceMOhm) {
            if (lineResistanceMOhm < 0) throw new IllegalArgumentException("Invalid line resistance");
            return (int) jouleLoss(currentMa, seriesResistance(FE_PORT_INTERNAL_RESISTANCE_MOHM, lineResistanceMOhm));
        }
    }

    /** Quote before calling the receiver, then recompute the loss from confirmed FE only. */
    public static final class FeQuote {
        private final int offered;
        private final double totalResistance;

        private FeQuote(int offered, double totalResistance) {
            this.offered = offered;
            this.totalResistance = totalResistance;
        }

        public int offered() { return offered; }

        public FeTransfer accepted(int amount) {
            if (amount < 0 || amount > offered) throw new IllegalArgumentException("Invalid FE acceptance");
            double current = lowerCurrentForLoad(amount, NOMINAL_VOLTAGE_MV, totalResistance);
            int loss = (int) jouleLoss(current, totalResistance);
            return new FeTransfer(amount, loss, current, amount == 0 ? NetworkStatus.IDLE : NetworkStatus.ACTIVE);
        }
    }

    private ElectricalSolver() {}

    public static FeQuote quoteFeTransfer(int extractionBudget, int receiverDemand, int lineResistanceMOhm) {
        if (extractionBudget < 0 || receiverDemand < 0 || lineResistanceMOhm < 0) {
            throw new IllegalArgumentException("Invalid FE route");
        }
        double resistance = seriesResistance(FE_PORT_INTERNAL_RESISTANCE_MOHM, lineResistanceMOhm);
        int low = 0;
        int high = Math.min(extractionBudget, receiverDemand);
        while (low < high) {
            int candidate = low + (int) (((long) high - low + 1) / 2);
            double current = lowerCurrentForLoad(candidate, NOMINAL_VOLTAGE_MV, resistance);
            if (current <= ElectricalMaterial.COPPER.currentLimitMa()
                    && Math.floor(jouleLoss(current, resistance)) <= extractionBudget - candidate) {
                low = candidate;
            } else {
                high = candidate - 1;
            }
        }
        return new FeQuote(low, resistance);
    }

    public static int feSourceTerminalVoltageMv(double currentMa) {
        return NOMINAL_VOLTAGE_MV - (int) Math.round(voltageDropMv(currentMa, FE_PORT_INTERNAL_RESISTANCE_MOHM));
    }

    /**
     * Independent source internal resistances, reverse-blocking source ports, directed resistors,
     * and passive loads solve against a shared ideal return. List order is preserved in the result.
     */
    public static ElectricalNetworkState solve(int nodeCount, List<Source> sources, List<Load> loads, List<Edge> edges) {
        if (nodeCount < 0) throw new IllegalArgumentException("Negative node count");
        if (nodeCount > MAX_NODES || edges.size() > MAX_EDGES
                || sources.size() > MAX_NODES || loads.size() > MAX_NODES) return overBudget();
        var enabledEdges = new boolean[edges.size()];
        var enabledSources = new boolean[sources.size()];
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            checkNode(edge.from(), nodeCount);
            checkNode(edge.to(), nodeCount);
            enabledEdges[i] = edge.enabled();
        }
        for (int i = 0; i < sources.size(); i++) {
            Source source = sources.get(i);
            checkNode(source.node(), nodeCount);
            enabledSources[i] = source.enabled();
        }
        for (Load load : loads) checkNode(load.node(), nodeCount);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double[] voltage = solveVoltages(nodeCount, sources, loads, edges, enabledSources, enabledEdges);
            if (voltage == null) return overBudget();
            if (!updateActivePorts(voltage, sources, edges, enabledSources, enabledEdges)) {
                return measure(voltage, sources, loads, edges, enabledSources, enabledEdges);
            }
        }
        return overBudget();
    }

    private static void checkNode(int node, int count) {
        if (node >= count) throw new IllegalArgumentException("Element outside the network");
    }

    private static ElectricalNetworkState overBudget() {
        return new ElectricalNetworkState(NetworkStatus.SOLVER_BUDGET_EXCEEDED,
                List.of(), List.of(), List.of(), List.of(), 0, 0, 0);
    }

    private static double[] solveVoltages(int count, List<Source> sources, List<Load> loads, List<Edge> edges,
                                         boolean[] enabledSources, boolean[] enabledEdges) {
        double[][] matrix = new double[count][count];
        double[] voltage = new double[count];
        int[] components = components(count, edges, enabledEdges);
        boolean[] referenced = new boolean[count];
        for (int i = 0; i < edges.size(); i++) {
            if (!enabledEdges[i]) continue;
            Edge edge = edges.get(i);
            int a = edge.from(), b = edge.to();
            double g = 1.0 / edge.resistanceMOhm();
            matrix[a][a] += g;
            matrix[b][b] += g;
            matrix[a][b] -= g;
            matrix[b][a] -= g;
        }
        for (Load load : loads) {
            matrix[load.node()][load.node()] += 1 / load.effectiveResistance();
            referenced[components[load.node()]] = true;
        }
        for (int i = 0; i < sources.size(); i++) {
            if (!enabledSources[i]) continue;
            Source source = sources.get(i);
            int node = source.node();
            matrix[node][node] += 1.0 / source.internalResistanceMOhm();
            voltage[node] += (double) source.voltageMv() / source.internalResistanceMOhm();
            referenced[components[node]] = true;
        }
        // Pin one node of each floating component. With no source or load, its current is zero.
        for (int n = 0; n < count; n++) {
            if (!referenced[n] && components[n] == n) matrix[n][n] += 1;
        }

        // In-place Cholesky factorization of the symmetric conductance matrix.
        for (int row = 0; row < count; row++) {
            for (int col = 0; col <= row; col++) {
                double entry = matrix[row][col];
                for (int k = 0; k < col; k++) entry -= matrix[row][k] * matrix[col][k];
                if (row == col) {
                    if (!(entry > 0) || !Double.isFinite(entry)) return null;
                    matrix[row][row] = Math.sqrt(entry);
                } else {
                    matrix[row][col] = entry / matrix[col][col];
                }
            }
            for (int k = 0; k < row; k++) voltage[row] -= matrix[row][k] * voltage[k];
            voltage[row] /= matrix[row][row];
        }
        for (int row = count - 1; row >= 0; row--) {
            for (int k = row + 1; k < count; k++) voltage[row] -= matrix[k][row] * voltage[k];
            voltage[row] /= matrix[row][row];
        }
        return voltage;
    }

    private static int[] components(int count, List<Edge> edges, boolean[] enabled) {
        var parent = new int[count];
        for (int i = 0; i < count; i++) parent[i] = i;
        for (int i = 0; i < edges.size(); i++) {
            if (!enabled[i]) continue;
            var edge = edges.get(i);
            parent[root(parent, edge.from())] = root(parent, edge.to());
        }
        for (int i = 0; i < count; i++) parent[i] = root(parent, i);
        return parent;
    }

    private static int root(int[] parent, int node) {
        while (parent[node] != node) node = parent[node];
        return node;
    }

    private static boolean updateActivePorts(double[] voltage, List<Source> sources, List<Edge> edges,
                                             boolean[] enabledSources, boolean[] enabledEdges) {
        boolean changed = false;
        for (int i = 0; i < sources.size(); i++) {
            Source source = sources.get(i);
            double drop = source.voltageMv() - voltage[source.node()];
            if (!source.enabled() || Math.abs(drop) <= VOLTAGE_TOLERANCE_MV) continue;
            boolean enabled = drop > 0;
            changed |= enabledSources[i] != enabled;
            enabledSources[i] = enabled;
        }
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            double drop = voltage[edge.from()] - voltage[edge.to()];
            if (!edge.enabled() || Math.abs(drop) <= VOLTAGE_TOLERANCE_MV) continue;
            boolean enabled = drop > 0 ? edge.forwards() : edge.backwards();
            changed |= enabledEdges[i] != enabled;
            enabledEdges[i] = enabled;
        }
        return changed;
    }

    private static ElectricalNetworkState measure(double[] voltage, List<Source> sources, List<Load> loads,
                                                  List<Edge> edges, boolean[] enabledSources, boolean[] enabledEdges) {
        // Ports that are reverse-blocked must stay blocked after voltage limiting.
        // Apply the same scale across their endpoints to preserve the voltage ordering.
        boolean[] connectedEdges = new boolean[edges.size()];
        for (int i = 0; i < edges.size(); i++) connectedEdges[i] = edges.get(i).enabled();
        int[] components = components(voltage.length, edges, connectedEdges);
        double[] scale = new double[voltage.length];
        Arrays.fill(scale, 1);
        for (int i = 0; i < sources.size(); i++) {
            if (!enabledSources[i]) continue;
            Source source = sources.get(i);
            double current = currentMa(source.voltageMv() - voltage[source.node()], source.internalResistanceMOhm());
            constrain(scale, components[source.node()], current, powerFePerTick(source.voltageMv(), current),
                    source.maxCurrentMa(), source.availableFePerTick());
        }
        for (int i = 0; i < edges.size(); i++) {
            if (!enabledEdges[i]) continue;
            Edge edge = edges.get(i);
            double current = Math.abs(currentMa(voltage[edge.from()] - voltage[edge.to()], edge.resistanceMOhm()));
            constrain(scale, components[edge.from()], current,
                    powerFePerTick(Math.max(voltage[edge.from()], voltage[edge.to()]), current),
                    edge.maxCurrentMa(), edge.availableFePerTick());
        }

        var sourceStates = new ArrayList<ElectricalNodeState>();
        var edgeStates = new ArrayList<ElectricalNodeState>();
        var loadStates = new ArrayList<ElectricalNodeState>();
        var nodeStates = new ArrayList<ElectricalNodeState>();
        var incoming = new double[voltage.length];
        var outgoing = new double[voltage.length];
        double extracted = 0, delivered = 0, totalLoss = 0;
        boolean shorted = false, undervoltage = false, open = false;
        for (int i = 0; i < voltage.length; i++) voltage[i] *= scale[components[i]];
        for (int i = 0; i < sources.size(); i++) {
            Source source = sources.get(i);
            int node = source.node();
            double emf = source.voltageMv() * scale[components[node]];
            double current = enabledSources[i] ? Math.max(0, currentMa(emf - voltage[node], source.internalResistanceMOhm())) : 0;
            double power = powerFePerTick(emf, current);
            double loss = jouleLoss(current, source.internalResistanceMOhm());
            extracted += power;
            totalLoss += loss;
            incoming[node] += current;
            sourceStates.add(new ElectricalNodeState(voltage[node], current, source.internalResistanceMOhm(),
                    power, loss, flowStatus(current, scale[components[node]], !enabledSources[i])));
        }
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            int a = edge.from(), b = edge.to();
            double current = enabledEdges[i] ? currentMa(voltage[a] - voltage[b], edge.resistanceMOhm()) : 0;
            outgoing[a] += Math.max(0, current);
            incoming[b] += Math.max(0, current);
            outgoing[b] += Math.max(0, -current);
            incoming[a] += Math.max(0, -current);
            double loss = jouleLoss(current, edge.resistanceMOhm());
            totalLoss += loss;
            edgeStates.add(new ElectricalNodeState(voltage[a], current, edge.resistanceMOhm(),
                    powerFePerTick(Math.max(voltage[a], voltage[b]), Math.abs(current)), loss,
                    flowStatus(current, scale[components[a]], !enabledEdges[i])));
        }
        for (Load load : loads) {
            int node = load.node();
            double current = Math.max(0, currentMa(voltage[node], load.effectiveResistance()));
            double power = powerFePerTick(voltage[node], current);
            boolean fault = current > CURRENT_TOLERANCE_MA && load.resistanceMOhm() == 0;
            boolean low = current > CURRENT_TOLERANCE_MA && voltage[node] < load.minimumVoltageMv();
            shorted |= fault;
            undervoltage |= low;
            open |= current <= CURRENT_TOLERANCE_MA;
            outgoing[node] += current;
            if (fault) totalLoss += power;
            else delivered += power;
            NetworkStatus status = fault ? NetworkStatus.SHORT_CIRCUIT_LIMITED : low ? NetworkStatus.UNDERVOLTAGE
                    : flowStatus(current, scale[components[node]], current <= CURRENT_TOLERANCE_MA);
            loadStates.add(new ElectricalNodeState(voltage[node], current, load.resistanceMOhm(),
                    fault ? 0 : power, fault ? power : 0, status));
        }
        for (int i = 0; i < voltage.length; i++) {
            double current = Math.max(incoming[i], outgoing[i]);
            if (Math.abs(incoming[i] - outgoing[i]) > CURRENT_TOLERANCE_MA * Math.max(1, current)) return overBudget();
            nodeStates.add(new ElectricalNodeState(voltage[i], current, 0, powerFePerTick(voltage[i], current),
                    0, flowStatus(current, scale[components[i]], false)));
        }
        if (!isBalanced(extracted, delivered, totalLoss)) return overBudget();
        NetworkStatus status = NetworkStatus.IDLE;
        if (shorted) status = NetworkStatus.SHORT_CIRCUIT_LIMITED;
        else if (Arrays.stream(scale).anyMatch(value -> value < 1)) status = NetworkStatus.CURRENT_LIMITED;
        else if (undervoltage) status = NetworkStatus.UNDERVOLTAGE;
        else if (extracted > 1e-8) status = NetworkStatus.ACTIVE;
        else if (sources.stream().anyMatch(Source::enabled) && (open || loads.isEmpty())) status = NetworkStatus.OPEN_CIRCUIT;
        return new ElectricalNetworkState(status, nodeStates, edgeStates, sourceStates, loadStates,
                extracted, delivered, totalLoss);
    }

    private static void constrain(double[] scale, int component, double current, double power, int maxMa, int maxFe) {
        if (current > maxMa) scale[component] = Math.min(scale[component], maxMa / current);
        if (power > maxFe) scale[component] = Math.min(scale[component], Math.sqrt(maxFe / power));
    }

    private static NetworkStatus flowStatus(double current, double scale, boolean open) {
        if (open) return NetworkStatus.OPEN_CIRCUIT;
        if (Math.abs(current) <= CURRENT_TOLERANCE_MA) return NetworkStatus.IDLE;
        return scale < 1 ? NetworkStatus.CURRENT_LIMITED : NetworkStatus.ACTIVE;
    }
}
