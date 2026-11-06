package br.com.quantumindustries.material;

import br.com.quantumindustries.assay.AssayCatalog.Material;
import java.util.Map;
import java.util.Set;

/** Qualitative process suitability; scientific discoveries remain in AssayProfile. */
public final class MaterialProcesses {
    public enum Property { MALLEABLE, DUCTILE, HARD, STRUCTURAL, CONDUCTIVE, THERMAL, MAGNETIC, WEAR_RESISTANT }
    public enum Process { STRUCTURE, TRANSMISSION, SHEET, WIRE }
    public record Profile(Set<Property> properties, Set<Process> processes) {
        public Profile { properties = Set.copyOf(properties); processes = Set.copyOf(processes); }
    }

    private static final Map<Material, Profile> METALS = Map.of(
            Material.IRON, new Profile(Set.of(Property.MALLEABLE, Property.HARD, Property.STRUCTURAL,
                    Property.THERMAL, Property.MAGNETIC, Property.WEAR_RESISTANT),
                    Set.of(Process.STRUCTURE, Process.TRANSMISSION, Process.SHEET)),
            Material.COPPER, new Profile(Set.of(Property.MALLEABLE, Property.DUCTILE, Property.CONDUCTIVE,
                    Property.THERMAL), Set.of(Process.SHEET, Process.WIRE)),
            Material.GOLD, new Profile(Set.of(Property.MALLEABLE, Property.DUCTILE, Property.CONDUCTIVE),
                    Set.of(Process.SHEET, Process.WIRE)));

    private MaterialProcesses() {}

    public static Map<Material, Profile> metals() { return METALS; }
    public static boolean supports(Material material, Process process) {
        Profile profile = METALS.get(material);
        return profile != null && profile.processes().contains(process);
    }
}
