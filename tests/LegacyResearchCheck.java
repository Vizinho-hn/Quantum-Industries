package br.com.quantumindustries.research;

import java.util.Set;

final class LegacyResearchCheck {
    public static void main(String[] args) {
        String[] expected = {"conductivity", "copper_wire", "material_observation",
                "material_malleability", "manual_forming", "metal_sheets",
                "electrical_insulation", "basic_mechanics", "mechanical_transmission",
                "basic_magnetism", "copper_winding", "electromechanical_structure",
                "manual_dynamo_design", "electromagnetic_induction"};
        for (int i = 0; i < expected.length; i++) {
            if (!LegacyResearch.decode(1 << i).equals(Set.of(expected[i]))
                    || !LegacyResearch.key(i).equals(expected[i])) throw new AssertionError(expected[i]);
        }
        if (!LegacyResearch.decode(0).isEmpty() || !LegacyResearch.decode(1 << 31).isEmpty()
                || !LegacyResearch.key(-1).isEmpty()
                || !LegacyResearch.decode(3).equals(Set.of("conductivity", "copper_wire"))) {
            throw new AssertionError("Invalid legacy decode");
        }
    }
}
