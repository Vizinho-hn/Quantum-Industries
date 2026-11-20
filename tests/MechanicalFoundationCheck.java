package br.com.quantumindustries.mechanical;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.HashMap;

public final class MechanicalFoundationCheck {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        var graph = Map.of(
                0, new MechanicalNetwork.Node<>(List.of(1,2),18,0),
                1, new MechanicalNetwork.Node<>(List.of(0,2,3),0,6),
                2, new MechanicalNetwork.Node<>(List.of(0,1,3),0,6),
                3, new MechanicalNetwork.Node<>(List.of(1,2),0,6));
        var cycle = MechanicalNetwork.evaluate(0, graph::get, 256);
        check(cycle.powered() && cycle.capacity()==18 && cycle.load()==18 && cycle.nodes().size()==4, "cycles must not duplicate capacity");
        for (int start : graph.keySet()) {
            var result = MechanicalNetwork.evaluate(start, graph::get, 256);
            check(result.capacity()==18 && result.load()==18, "evaluation must not depend on caller order");
        }
        var overload = MechanicalNetwork.evaluate(0, n -> n==0 ? new MechanicalNetwork.Node<>(List.of(1),6,0)
                : new MechanicalNetwork.Node<>(List.of(0),0,12),256);
        check(!overload.powered(), "underpowered network stalls instead of powering every branch");
        var twoSources=MechanicalNetwork.evaluate(0,n->new MechanicalNetwork.Node<>(List.of(1-n),6,n==0?12:0),256);
        check(twoSources.powered() && twoSources.capacity()==12,"distinct sources may combine only once");
        check(MechanicalNetwork.evaluate(0, graph::get,2).limited(),"oversized graphs fail closed");
        var chain=MechanicalNetwork.evaluate(0,n->new MechanicalNetwork.Node<>(List.of(n+1),n==0?18:0,0),256);
        check(chain.limited() && chain.nodes().size()==256 && !chain.powered(),"unbounded topology cannot run forever");
        var missing=MechanicalNetwork.evaluate(0,n->n==0?new MechanicalNetwork.Node<>(List.of(1),0,6):null,256);
        check(!missing.powered() && missing.nodes().size()==1,"unloaded neighbor cannot become a source");

        // Hydraulic torque matrix (Cases A-J): tau = (r x F) . axis_hat
        var caseA = HydraulicTorque.evaluate(Direction.Axis.Z, List.of(new HydraulicTorque.Sample(0, -1, 0, 1.0, 0.0, 0.0)));
        var caseB = HydraulicTorque.evaluate(Direction.Axis.Z, List.of(new HydraulicTorque.Sample(0, -1, 0, -1.0, 0.0, 0.0)));
        var caseC = HydraulicTorque.evaluate(Direction.Axis.Z, List.of(new HydraulicTorque.Sample(0, 1, 0, 1.0, 0.0, 0.0)));
        var caseD = HydraulicTorque.evaluate(Direction.Axis.Z, List.of(new HydraulicTorque.Sample(0, 1, 0, 0.0, -1.0, 0.0)));
        var caseE = HydraulicTorque.evaluate(Direction.Axis.Z, List.of());
        var caseF = HydraulicTorque.evaluate(Direction.Axis.Z, List.of(
                new HydraulicTorque.Sample(0, -1, 0, 1.0, 0.0, 0.0),
                new HydraulicTorque.Sample(0, 1, 0, 1.0, 0.0, 0.0)));
        var caseG = HydraulicTorque.evaluate(Direction.Axis.Z, List.of(
                new HydraulicTorque.Sample(0, -1, 0, 1.0, 0.0, 0.0),
                new HydraulicTorque.Sample(0, 1, 0, -1.0, 0.0, 0.0)));
        var caseH = HydraulicTorque.evaluate(Direction.Axis.Z, List.of(new HydraulicTorque.Sample(1, 0, 0, 0.0, -1.0, 0.0)));
        var caseI = HydraulicTorque.evaluate(Direction.Axis.Z, List.of(new HydraulicTorque.Sample(-1, 0, 0, 0.0, -1.0, 0.0)));
        var caseJx = HydraulicTorque.evaluate(Direction.Axis.X, List.of(new HydraulicTorque.Sample(0, 0, 1, 0.0, -1.0, 0.0)));
        var caseJxOpp = HydraulicTorque.evaluate(Direction.Axis.X, List.of(new HydraulicTorque.Sample(0, 0, -1, 0.0, -1.0, 0.0)));

        check(caseA.active() && caseA.torque() > 0 && caseA.direction() == 1, "Case A: tangential flow must produce +T");
        check(caseB.active() && caseB.torque() < 0 && caseB.direction() == -1, "Case B: inverted flow must invert sign");
        check(caseC.active() && caseC.torque() < 0 && caseC.direction() == -1, "Case C: opposite side must invert sign");
        check(!caseD.active() && Math.abs(caseD.torque()) <= HydraulicTorque.EPSILON, "Case D: radial flow must not spin wheel");
        check(!caseE.active() && caseE.torque() == 0.0, "Case E: no water must yield zero torque");
        check(!caseF.active() && Math.abs(caseF.torque()) <= HydraulicTorque.EPSILON, "Case F: balanced opposite flows must stall");
        check(caseG.active() && Math.abs(caseG.torque() - 2.0 * caseA.torque()) < 1e-6 && caseG.direction() == caseA.direction(),
                "Case G: reinforcing flows must add magnitude with same sign");
        check(caseH.active() && caseI.active() && caseH.direction() == -caseI.direction(),
                "Cases H/I: vertical fall on opposite halves must spin in opposite directions");
        check(caseJx.active() && caseJxOpp.active() && caseJx.direction() == -caseJxOpp.direction(),
                "Case J: 90-degree rotated wheel (Axis.X) must preserve signed torque invariants");

        // KineticMath determinism & facing-sign invariants
        float cw10 = KineticMath.deterministicAngle(10.0, 0f, true, 1, 9.0f);
        float ccw10 = KineticMath.deterministicAngle(10.0, 0f, true, -1, 9.0f);
        check(Math.abs(cw10 - 90.0f) < 1e-4f && Math.abs(ccw10 - 270.0f) < 1e-4f,
                "KineticMath must advance deterministically in opposite directions");
        check(KineticMath.localRotorSign(Direction.NORTH) == -KineticMath.localRotorSign(Direction.SOUTH),
                "Opposite facings on Z axis must have opposite local rotor signs");
        check(KineticMath.localRotorSign(Direction.WEST) == -KineticMath.localRotorSign(Direction.EAST),
                "Opposite facings on X axis must have opposite local rotor signs");

        verifyTransmissions();
        verifyKinetics();
        br.com.quantumindustries.client.kinetic.QksGeometrySelfCheck.verifyAll();
        verifyNativeRotorSigns();

        System.out.println("PASS: bounded mechanics, hydraulic torque matrix (A-J), kinetic phase math, conservation of capacity, native geometry, and rotor signs");
    }

    private static void verifyTransmissions() {
        BlockPos second = ROOT.east();
        check(BeltGeometry.validAlignment(ROOT, ROOT.east(2), Direction.Axis.Z), "belt accepts minimum straight perpendicular span");
        check(BeltGeometry.validAlignment(ROOT, ROOT.above(12), Direction.Axis.X), "belt accepts maximum vertical span");
        check(!BeltGeometry.validAlignment(ROOT, ROOT.east(), Direction.Axis.Z), "belt rejects adjacent endpoints");
        check(!BeltGeometry.validAlignment(ROOT, ROOT.east(13), Direction.Axis.Z), "belt rejects excessive distance");
        check(!BeltGeometry.validAlignment(ROOT, ROOT.east(2).above(), Direction.Axis.Z), "belt rejects diagonal endpoints");
        check(!BeltGeometry.validAlignment(ROOT, ROOT.east(2), Direction.Axis.X), "belt rejects axial span");
        for (double ratio : new double[] {-.5, -2, 1, -1}) {
            double efficiency = Math.abs(ratio) == 1 ? .94 : .96;
            var graph = Map.of(ROOT, rotor(List.of(new KineticLink(second, ratio, efficiency)), 18, 0, 18, .9, .05, 0),
                    second, rotor(List.of(new KineticLink(ROOT, 1 / ratio, efficiency)), 0, 0, 0, 2.4, .08, 0));
            var topology = KineticNetworkManager.evaluate(ROOT, graph::get, 256);
            check(!topology.jammed() && topology.members().get(second).ratio() == ratio, "reciprocal gears and belts preserve ratio and spin");
            double reflected = KineticDynamics.reflectLoadTorque(6, ratio, efficiency);
            check(reflected > 6 * Math.abs(ratio), "transmission efficiency loses power instead of creating energy");
            check(Math.abs(KineticDynamics.reflectDriveTorque(6, ratio, efficiency)) < 6 * Math.abs(ratio), "drive reflection loses power");
            var jamGraph = Map.of(ROOT, graph.get(ROOT), second, rotor(List.of(new KineticLink(ROOT, 1, efficiency)), 0, 0, 0, 2.4, .08, 0));
            if (ratio != 1) check(run(KineticNetworkManager.evaluate(ROOT, jamGraph::get, 256), 1).reason() == StopReason.JAMMED,
                    "inconsistent transmission cycle must jam");
        }
        var light = KineticNetworkManager.evaluate(ROOT, pos -> rotor(List.of(), 18, 0, 18, 1, .06, 0), 256);
        var heavy = KineticNetworkManager.evaluate(ROOT, pos -> rotor(List.of(), 18, 0, 18, 19, .12, 0), 256);
        check(run(heavy, 1).omega() < run(light, 1).omega(), "flywheel smooths acceleration");
        var coastLight = KineticNetworkManager.evaluate(ROOT, pos -> rotor(List.of(), 0, 0, 0, 1, .06, 2), 256);
        var coastHeavy = KineticNetworkManager.evaluate(ROOT, pos -> rotor(List.of(), 0, 0, 0, 19, .12, 2), 256);
        check(run(coastHeavy, 20).omega() > run(coastLight, 20).omega(), "flywheel coasts longer");
        check(!run(coastHeavy, 20).powered() && run(coastHeavy, 20).omega() < 2, "coasting never creates energy or production");
        check(run(KineticNetworkManager.evaluate(ROOT, pos -> rotor(List.of(), 0, 0, 0, 18, .06, 0), 256), 100).omega() == 0,
                "unpowered flywheel cannot start itself");
        for (int radius : new int[] {1, 2}) {
            int capacity = radius == 1 ? 36 : 54;
            for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                var samples = new ArrayList<HydraulicTorque.Sample>();
                for (int horizontal = -radius; horizontal <= radius; horizontal++) {
                    for (int vertical = -radius; vertical <= radius; vertical++) {
                        if (Math.max(Math.abs(horizontal), Math.abs(vertical)) != radius) continue;
                        double fx = axis == Direction.Axis.Z ? -(double) vertical / radius : 0;
                        double fz = axis == Direction.Axis.X ? (double) vertical / radius : 0;
                        double fy = (axis == Direction.Axis.Z ? (double) horizontal : -(double) horizontal) / radius;
                        samples.add(new HydraulicTorque.Sample(axis == Direction.Axis.Z ? horizontal : 0, vertical,
                                axis == Direction.Axis.X ? horizontal : 0, fx, fy, fz));
                    }
                }
                var full = HydraulicTorque.evaluateRim(axis, samples, radius, capacity);
                var partial = HydraulicTorque.evaluateRim(axis, List.of(samples.getFirst()), radius, capacity);
                check(full.capacity() == capacity && Math.abs(full.torque() - capacity) < 1e-6, "fully wetted rim reaches rated torque and UM");
                check(partial.capacity() > 0 && partial.capacity() < capacity && partial.torque() > 0 && partial.torque() < full.torque(),
                        "one water cell gives proportional torque and UM on both wheel axes");
                check(Math.abs(partial.torque() * 8 * radius - full.torque()) < 1e-6, "one fully driven cell contributes exactly its share of torque");
                var dry = HydraulicTorque.evaluateRim(axis, List.of(), radius, capacity);
                check(dry.capacity() == 0 && dry.torque() == 0, "dry rim has no torque or capacity");
                var opposite = samples.stream().map(v -> new HydraulicTorque.Sample(v.rx(), v.ry(), v.rz(), -v.fx(), -v.fy(), -v.fz())).toList();
                check(HydraulicTorque.evaluateRim(axis, opposite, radius, capacity).torque() == -full.torque(), "rim flow reversal preserves primitive wheel sign");
            }
        }
    }

    private static void verifyNativeRotorSigns() {
        for (Direction facing : Direction.values()) {
            var orientation = new org.joml.Quaternionf();
            if (facing == Direction.UP) orientation.rotationX((float) Math.PI / 2);
            else if (facing == Direction.DOWN) continue; // No downward-facing shaft model.
            else orientation.rotationY((float) Math.toRadians(switch (facing) {
                case EAST -> -90; case SOUTH -> 180; case WEST -> 90; default -> 0;
            }));
            for (Direction.Axis local : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                if (local == Direction.Axis.X && facing.getAxis() == Direction.Axis.Y) continue;
                float sign = br.com.quantumindustries.client.kinetic.QksBlockEntityRenderer
                        .worldAxisSign(local, facing);
                var axis = local == Direction.Axis.X ? new org.joml.Vector3f(sign, 0, 0)
                        : new org.joml.Vector3f(0, 0, sign);
                orientation.transform(axis);
                check(Math.abs(axis.x + axis.y + axis.z - 1) < 1e-5,
                        "Native rotor must follow positive world-axis right-hand rule: " + local + "/" + facing);
                var opposite = new org.joml.Vector3f(axis).mul(-1);
                check(Math.abs(opposite.x + opposite.y + opposite.z + 1) < 1e-5,
                        "Negative angular velocity must reverse the native rotor");
            }
        }
        boolean rejected = false;
        try {
            br.com.quantumindustries.client.kinetic.QksBlockEntityRenderer
                    .worldAxisSign(Direction.Axis.Y, Direction.NORTH);
        } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Native renderer rejects unsupported local rotor axes");
    }

    private static final BlockPos ROOT = BlockPos.ZERO;
    private static KineticNetworkManager.Node rotor(List<KineticLink> links, int capacity, int load,
            double drive, double inertia, double drag, double omega) {
        return new KineticNetworkManager.Node(links, capacity, load, inertia, drag, drive, load * .75,
                .08, 12, 0, omega);
    }
    private static Map<BlockPos, KineticNetworkManager.Node> sawmills(int count, boolean wet, double direction) {
        var graph = new HashMap<BlockPos, KineticNetworkManager.Node>();
        var links = new ArrayList<KineticLink>();
        for (int i = 1; i <= count; i++) {
            var pos = new BlockPos(i, 0, 0);
            links.add(new KineticLink(pos, 1, 1));
            graph.put(pos, rotor(List.of(new KineticLink(ROOT, 1, 1)), 0, 6, 0, 2.5, .2, 0));
        }
        graph.put(ROOT, rotor(links, wet ? 18 : 0, 0, wet ? 18 * direction : 0, 4.5, wet ? 4.5 : .65, 0));
        return graph;
    }
    private static KineticNetworkManager.Motion run(KineticNetworkManager.Topology topology, int ticks) {
        var motion = new KineticNetworkManager.Motion(topology.initialAngle(), topology.initialOmega(), 0, 0, false, StopReason.NONE);
        for (int tick = 0; tick < ticks; tick++) motion = KineticNetworkManager.advance(topology, motion);
        return motion;
    }
    private static void verifyKinetics() {
        var oneGraph = sawmills(1, true, 1);
        var one = KineticNetworkManager.evaluate(ROOT, oneGraph::get, 256);
        var initial = run(one, 1);
        var accelerated = run(one, 20);
        check(initial.omega() > 0 && accelerated.omega() > initial.omega(), "network starts progressively");
        var threeGraph = sawmills(3, true, 1);
        var three = KineticNetworkManager.evaluate(ROOT, threeGraph::get, 256);
        check(run(three, 400).powered() && run(three, 400).omega() < run(one, 400).omega(), "three sawmills run more slowly than one");
        var fourGraph = sawmills(4, true, 1);
        var four = KineticNetworkManager.evaluate(ROOT, fourGraph::get, 256);
        var stalled = run(four, 100);
        check(stalled.omega() == 0 && !stalled.powered() && stalled.reason() == StopReason.OVERLOADED, "four sawmills stall without producing");
        var dryGraph = sawmills(1, false, 1);
        var dry = KineticNetworkManager.evaluate(ROOT, dryGraph::get, 256);
        var coasting = KineticNetworkManager.advance(dry, accelerated);
        check(coasting.omega() > 0 && coasting.omega() < accelerated.omega() && !coasting.powered(), "water removal coasts without allowing production");
        var reversedGraph = sawmills(1, true, -1);
        var reversed = KineticNetworkManager.evaluate(ROOT, reversedGraph::get, 256);
        var reversing = accelerated;
        boolean crossed = false;
        for (int tick = 0; tick < 200; tick++) {
            double old = reversing.omega();
            reversing = KineticNetworkManager.advance(reversed, reversing);
            if (old >= 0 && reversing.omega() < 0) crossed = true;
        }
        check(crossed && reversing.omega() < 0, "torque reversal passes through zero");
        var negativeCoasting = KineticNetworkManager.advance(dry, reversing);
        check(dry.drive() == 0 && negativeCoasting.omega() < 0
                && negativeCoasting.omega() > reversing.omega() && !negativeCoasting.powered(),
                "negative rotation coasts toward rest without power after water removal");
        // Zero drive has a positive fallback, but residual velocity must determine the snapshot sign.
        var coastingSnapshot = new KineticSnapshot(1, 1, 1,
                KineticDynamics.wrap(negativeCoasting.unwrappedAngle()), negativeCoasting.omega(), negativeCoasting.alpha(),
                dry.drive(), dry.resistance(), negativeCoasting.netTorque(), dry.inertia(), 1,
                dry.capacity(), dry.load(), dry.members().size(), 1,
                negativeCoasting.powered(), dry.limited(), dry.jammed(), negativeCoasting.reason());
        check(coastingSnapshot.angularVelocity() < 0 && coastingSnapshot.spinDirection() == -1
                && coastingSnapshot.stopReason() == StopReason.NO_SOURCE,
                "negative coasting snapshot preserves spin direction after water removal");
        var second = new BlockPos(1, 0, 0);
        var opposing = Map.of(ROOT, rotor(List.of(new KineticLink(second, 1, 1)), 18, 0, 18, 4.5, 4.5, 0),
                second, rotor(List.of(new KineticLink(ROOT, 1, 1)), 18, 0, -18, 4.5, 4.5, 0));
        var balanced = run(KineticNetworkManager.evaluate(ROOT, opposing::get, 256), 100);
        check(!balanced.powered() && balanced.omega() == 0 && balanced.reason() == StopReason.OPPOSING_SOURCES, "opposing wheels cancel");
        var contradiction = Map.of(ROOT, rotor(List.of(new KineticLink(second, 2, 1)), 18, 0, 18, 4.5, 4.5, 0),
                second, rotor(List.of(new KineticLink(ROOT, 1, 1)), 0, 0, 0, 1, .1, 0));
        var jam = run(KineticNetworkManager.evaluate(ROOT, contradiction::get, 256), 1);
        check(!jam.powered() && jam.omega() == 0 && jam.reason() == StopReason.JAMMED, "contradictory ratio cycle jams");
        var merged = Map.of(ROOT, rotor(List.of(new KineticLink(second, 2, 1)), 0, 0, 0, 4, .1, 3),
                second, rotor(List.of(new KineticLink(ROOT, .5, 1)), 0, 0, 0, 2, .1, -1));
        var merge = KineticNetworkManager.evaluate(ROOT, merged::get, 256);
        check(Math.abs(merge.initialOmega() - (12.0 - 4) / (4 + 8)) < 1e-8, "merge conserves reflected angular momentum");
        var split = KineticNetworkManager.evaluate(ROOT, pos -> rotor(List.of(), 0, 0, 0, 4, .1, 3), 256);
        check(Math.abs(split.initialOmega() - 3) < 1e-8, "split preserves member velocity");
        var wrapped = Map.of(ROOT, new KineticNetworkManager.Node(List.of(new KineticLink(second, 1, 1)), 0, 0, 4, .1, 0, 0, .08, 12, 6.27, 0),
                second, new KineticNetworkManager.Node(List.of(), 0, 0, 1, .1, 0, 0, .08, 12, .01, 0));
        check(Math.abs(KineticNetworkManager.evaluate(ROOT, wrapped::get, 256).initialAngle() - 6.27) < 1e-8,
                "topology initialization never averages angles across the wrap seam");
        check(!KineticNetworkManager.shouldSync(null, KineticSnapshot.EMPTY), "idle rotor sends no routine packets");
        var moving = new KineticSnapshot(1, 1, 5, 1, .01, 0, 0, 0, 0, 1, 1,
                0, 0, 1, 1, false, false, false, StopReason.NO_SOURCE);
        var stopped = new KineticSnapshot(1, 1, 6, 1, 0, 0, 0, 0, 0, 1, 1,
                0, 0, 1, 1, false, false, false, StopReason.NO_SOURCE);
        check(KineticNetworkManager.shouldSync(moving, stopped), "final stop is synchronized even below the speed threshold");
        check(!KineticNetworkManager.shouldSync(stopped, stopped), "settled network sends no duplicate idle packets");
        check(KineticNetworkManager.evaluate(ROOT, oneGraph::get, 1).limited(), "QKS traversal obeys its node bound");
        var bounded = run(KineticNetworkManager.evaluate(ROOT, oneGraph::get, 1), 20);
        check(!bounded.powered() && bounded.reason() == StopReason.NETWORK_LIMIT && bounded.omega() == 0,
                "bounded network cannot spin up or produce");
        boolean rejected = false;
        try { new KineticLink(ROOT, 0, 1); } catch (IllegalArgumentException error) { rejected = true; }
        check(rejected, "kinematic links reject zero ratios");
        rejected = false;
        try { new KineticLink(ROOT, 1, 1.1); } catch (IllegalArgumentException error) { rejected = true; }
        check(rejected, "kinematic links reject energy creating efficiency");
        for (int count : new int[] {1, 32, 128, 256}) {
            var graph = new HashMap<BlockPos, KineticNetworkManager.Node>();
            for (int i = 0; i < count; i++) {
                var links = new ArrayList<KineticLink>();
                if (i > 0) links.add(new KineticLink(new BlockPos(i - 1, 0, 0), 1, 1));
                if (i + 1 < count) links.add(new KineticLink(new BlockPos(i + 1, 0, 0), 1, 1));
                graph.put(new BlockPos(i, 0, 0), rotor(links, i == 0 ? 18 : 0, 0, i == 0 ? 18 : 0, .6, .1, 0));
            }
            var topology = KineticNetworkManager.evaluate(ROOT, graph::get, 256);
            check(topology.members().size() == count && !topology.limited(), "benchmark traverses every intended node");
            run(topology, 1000);
            long start = System.nanoTime();
            for (int repeat = 0; repeat < 100; repeat++) {
                var measured = KineticNetworkManager.evaluate(ROOT, graph::get, 256);
                check(run(measured, 1).powered(), "benchmark actually advances the solver");
            }
            System.out.printf("QKS benchmark: %d nodes, %.3f us/topology+tick%n", count, (System.nanoTime() - start) / 100_000.0);
        }
    }
}
