package br.com.quantumindustries.mechanical;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;

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

        System.out.println("PASS: bounded mechanics, hydraulic torque matrix (A-J), kinetic phase math, and conservation of capacity");
    }
}
