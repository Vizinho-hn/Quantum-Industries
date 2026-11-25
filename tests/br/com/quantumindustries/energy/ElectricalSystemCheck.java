package br.com.quantumindustries.energy;

import br.com.quantumindustries.energy.PhysicsEnergyContract.ConversionReceipt;
import br.com.quantumindustries.energy.PhysicsEnergyContract.EnergyDomain;
import br.com.quantumindustries.energy.qes.ElectricalMath;
import br.com.quantumindustries.energy.qes.ElectricalNetworkState;
import br.com.quantumindustries.energy.qes.ElectricalNetworkState.NetworkStatus;
import br.com.quantumindustries.energy.qes.ElectricalSolver;
import br.com.quantumindustries.energy.qes.ElectricalSolver.Edge;
import br.com.quantumindustries.energy.qes.ElectricalSolver.Load;
import br.com.quantumindustries.energy.qes.ElectricalSolver.PortMode;
import br.com.quantumindustries.energy.qes.ElectricalSolver.Source;
import java.util.ArrayList;
import java.util.List;

public final class ElectricalSystemCheck {
    private ElectricalSystemCheck() {}

    public static void main(String[] args) {
        var source = new Source(0, 120_000, 1_000, 10_000, 1_000);
        var load = new Load(1, 120_000, 0);
        var wire = edge(0, 1, 1_000);
        var single = solve(2, List.of(source), List.of(load), List.of(wire));
        check(single.status() == NetworkStatus.ACTIVE, "one source and one resistive load");
        near(single.edges().getFirst().currentMa(), 120_000_000.0 / 122_000, "Ohm");
        check(single.lossFePerTick() > 0, "Joule loss");
        near(single.edges().getFirst().lossFePerTick(),
                ElectricalMath.jouleLoss(single.edges().getFirst().currentMa(), 1_000), "line loss from current");
        near(single.extractedFePerTick(), single.deliveredFePerTick() + single.lossFePerTick(), "energy balance");

        var series = solve(3, List.of(source), List.of(new Load(2, 120_000, 0)),
                List.of(edge(0, 1, 1_000), edge(1, 2, 2_000)));
        near(ElectricalMath.seriesResistance(1_000, 2_000), 3_000, "series resistance");
        near(series.edges().getFirst().currentMa(), series.edges().getLast().currentMa(), "series KCL");
        var parallel = solve(2, List.of(source), List.of(load), List.of(wire, edge(0, 1, 2_000)));
        near(ElectricalMath.parallelResistance(1_000, 2_000), 2_000.0 / 3, "parallel resistance");
        near(parallel.edges().getFirst().currentMa(), 2 * parallel.edges().getLast().currentMa(), "parallel division");

        var branch = solve(4, List.of(source, new Source(1, 110_000, 20_000, 500, 25)),
                List.of(new Load(2, 40_000, 0), new Load(3, 120_000, 0)),
                List.of(edge(0, 2, 2_000), edge(1, 2, 2_000), edge(2, 3, 1_000)));
        check(branch.sources().get(0).powerFePerTick() > branch.sources().get(1).powerFePerTick()
                && branch.sources().get(1).powerFePerTick() > 0 && branch.sources().get(1).powerFePerTick() <= 25,
                "two sources with distinct voltage and available power");
        near(branch.edges().get(0).currentMa() + branch.edges().get(1).currentMa(),
                branch.loads().getFirst().currentMa() + branch.edges().get(2).currentMa(), "junction KCL");

        var bottleneck = solve(2, List.of(source), List.of(load), List.of(new Edge(0, 1, 1_000, 10_000, 8,
                PortMode.AUTO, PortMode.AUTO)));
        check(bottleneck.status() == NetworkStatus.CURRENT_LIMITED
                && bottleneck.edges().getFirst().powerFePerTick() <= 8.000001, "residual conductor power budget");
        var lowVoltage = solve(2, List.of(source), List.of(new Load(1, 120_000, 119_000)), List.of(wire));
        check(lowVoltage.status() == NetworkStatus.UNDERVOLTAGE, "resistive load voltage threshold");

        var open = solve(2, List.of(source), List.of(load), List.of(new Edge(0, 1, 1_000, 10_000, 64,
                PortMode.NONE, PortMode.AUTO)));
        check(open.status() == NetworkStatus.OPEN_CIRCUIT && open.extractedFePerTick() == 0, "open switch");
        var allowed = new Edge(0, 1, 1_000, 10_000, 1_000, PortMode.PUSH, PortMode.PULL);
        check(solve(2, List.of(source), List.of(load), List.of(allowed)).deliveredFePerTick() > 0, "PUSH to PULL");
        check(solve(2, List.of(new Source(1, 120_000, 1_000, 1_000, 100)),
                List.of(new Load(0, 120_000, 0)), List.of(allowed)).deliveredFePerTick() == 0, "PULL blocks reverse");

        var shorted = solve(3, List.of(source), List.of(new Load(2, 0, 0)),
                List.of(wire, edge(1, 2, 1_000), edge(2, 0, 9_000)));
        check(shorted.status() == NetworkStatus.SHORT_CIRCUIT_LIMITED && shorted.deliveredFePerTick() == 0
                && shorted.extractedFePerTick() <= source.availableFePerTick(), "safe short with a cycle");
        near(shorted.extractedFePerTick(), shorted.lossFePerTick(), "short is dissipative");
        var oversized = solve(ElectricalSolver.MAX_NODES + 1, List.of(source), List.of(load), List.of(wire));
        check(oversized.status() == NetworkStatus.SOLVER_BUDGET_EXCEEDED
                && oversized.extractedFePerTick() == 0, "budget exhaustion is safe");

        verifyFeAdapter();
        verifyConversionContract();
        for (int nodes : new int[]{8, 32, 128}) benchmark(nodes);
        System.out.println("PASS ElectricalSystemCheck");
    }

    private static void verifyFeAdapter() {
        var quote = ElectricalSolver.quoteFeTransfer(64, 64, 400);
        check(quote.offered() == 64, "legacy FE offer");
        var commit = quote.accepted(17);
        check(commit.delivered() == 17 && commit.extracted() == commit.delivered() + commit.loss(),
                "partial acceptance limits the debit");
        check(quote.accepted(0).extracted() == 0, "rejected FE never debited");
        expectInvalid(() -> quote.accepted(65));
        var resistive = ElectricalSolver.quoteFeTransfer(64, 64, 20_000).accepted(50);
        check(resistive.loss() > 0 && resistive.extracted() <= 64, "high resistance FE Joule debit");
        check(resistive.lossThrough(10_000) <= resistive.lossThrough(20_000)
                && resistive.lossThrough(20_000) == resistive.loss(), "line losses allocated exactly once");
        for (int budget = 0; budget <= 64; budget++) {
            var longRoute = ElectricalSolver.quoteFeTransfer(budget, 64, 448_000);
            for (int amount = 0; amount <= longRoute.offered(); amount++) {
                var confirmed = longRoute.accepted(amount);
                check(confirmed.extracted() <= budget && confirmed.delivered() + confirmed.loss() == confirmed.extracted(),
                        "any partial receiver acceptance preserves integer budget");
            }
        }

        int battery = commit.delivered();
        int load = ElectricalSolver.quoteFeTransfer(battery, battery, 400).offered();
        var discharge = ElectricalSolver.quoteFeTransfer(battery, battery, 400).accepted(load);
        battery -= discharge.extracted();
        check(battery == 0 && discharge.delivered() == 17, "stored FE charges and discharges without duplication");
        check(ElectricalSolver.quoteFeTransfer(battery, 64, 400).offered() == 0, "empty battery cannot discharge");
    }

    private static void verifyConversionContract() {
        var joule = PhysicsEnergyContract.jouleHeating(40);
        check(joule.usefulOutput() == joule.inputAmount() * PhysicsEnergyContract.TU_PER_FE, "pure Joule scale");
        check(PhysicsEnergyContract.isClosedLoopNonAmplifying(), "motor–generator loop");
        for (int rpm = 0; rpm <= 90; rpm++) {
            check(GeneratorModel.netRate(rpm) <= 8 * GeneratorModel.SCALE
                    && GeneratorModel.netRate(rpm) < PhysicsEnergyContract.MOTOR_COST * GeneratorModel.SCALE,
                    "generator curve cannot replenish a motor tick");
        }
        check(!PhysicsEnergyContract.isClosedLoopNonAmplifying(24, 18, 18, 25), "amplifying loop rejected");
        expectInvalid(() -> new ConversionReceipt(EnergyDomain.ELECTRICAL_QES, EnergyDomain.THERMAL_QTS,
                Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 1));
    }

    private static ElectricalNetworkState solve(int nodes, List<Source> sources, List<Load> loads, List<Edge> edges) {
        var state = ElectricalSolver.solve(nodes, sources, loads, edges);
        check(ElectricalMath.isBalanced(state.extractedFePerTick(), state.deliveredFePerTick(),
                state.lossFePerTick()), "network energy balance");
        return state;
    }

    private static Edge edge(int from, int to, int resistance) {
        return new Edge(from, to, resistance, 10_000, 1_000, PortMode.AUTO, PortMode.AUTO);
    }

    private static void benchmark(int count) {
        var edges = new ArrayList<Edge>();
        for (int i = 1; i < count; i++) {
            edges.add(edge(i - 1, i, 150));
            if (i > 1) edges.add(edge(i - 2, i, 400));
        }
        var sources = List.of(new Source(0, 120_000, 1_000, 1_000, 64));
        var loads = List.of(new Load(count - 1, 240_000, 0));
        var expected = solve(count, sources, loads, edges);
        check(expected.deliveredFePerTick() > 0, "benchmark circuit conducts");
        for (int i = 0; i < 10; i++) solve(count, sources, loads, edges);
        int runs = 30;
        long started = System.nanoTime();
        for (int i = 0; i < runs; i++) {
            check(expected.equals(solve(count, sources, loads, edges)), "deterministic network state");
        }
        System.out.printf("%d nodes, %d runs, %.3f ms/solve%n", count, runs,
                (System.nanoTime() - started) / (runs * 1_000_000.0));
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) <= 1e-6 * Math.max(1, Math.abs(expected)), message);
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("invalid input was accepted");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
