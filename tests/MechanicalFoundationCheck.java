package br.com.quantumindustries.mechanical;

import java.util.List;
import java.util.Map;

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
        System.out.println("PASS: bounded mechanics, cycles and conservation of capacity; material registries tested inside NeoForge");
    }
}
