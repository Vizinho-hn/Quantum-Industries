package br.com.quantumindustries.research;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Frozen save and report order from Era I. Never reorder this list. */
final class LegacyResearch {
    private static final List<String> IDS = List.of(
            "conductivity",
            "copper_wire",
            "material_observation",
            "material_malleability",
            "manual_forming",
            "metal_sheets",
            "electrical_insulation",
            "basic_mechanics",
            "mechanical_transmission",
            "basic_magnetism",
            "copper_winding",
            "electromechanical_structure",
            "manual_dynamo_design",
            "electromagnetic_induction"
    );

    private LegacyResearch() {}

    static String key(int oldIndex) {
        return oldIndex >= 0 && oldIndex < IDS.size() ? IDS.get(oldIndex) : "";
    }

    static Set<String> decode(int bits) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < IDS.size(); i++) {
            if ((bits & (1 << i)) != 0) result.add(IDS.get(i));
        }
        return result;
    }
}
