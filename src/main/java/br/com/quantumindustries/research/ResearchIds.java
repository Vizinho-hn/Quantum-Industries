package br.com.quantumindustries.research;

import br.com.quantumindustries.QuantumIndustries;
import net.minecraft.resources.Identifier;

public final class ResearchIds {
    private ResearchIds() {}

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(QuantumIndustries.MODID, path);
    }

    public static Identifier parse(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.indexOf(Identifier.NAMESPACE_SEPARATOR) < 0) {
            return Identifier.tryBuild(QuantumIndustries.MODID, value);
        }
        return Identifier.tryParse(value);
    }
}
