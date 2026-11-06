package br.com.quantumindustries.material;

import br.com.quantumindustries.assay.AssayCatalog.Material;
import java.util.Map;
import java.util.Set;

/** Qualitative process suitability; scientific discoveries remain in AssayProfile. */
public final class MaterialProcesses {
    public enum Property { MALLEABLE, DUCTILE, HARD, STRUCTURAL, CONDUCTIVE, THERMAL, MAGNETIC, WEAR_RESISTANT }
    public enum Process { STRUCTURE, TRANSMISSION, SHEET, WIRE, SAWING, ABRASIVE, FUEL }
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

    private static final Profile WOOD = new Profile(Set.of(Property.STRUCTURAL), Set.of(Process.STRUCTURE, Process.SAWING));
    private static final Profile STONE = new Profile(Set.of(Property.HARD, Property.WEAR_RESISTANT), Set.of(Process.STRUCTURE, Process.ABRASIVE));
    private static final Profile COAL = new Profile(Set.of(Property.THERMAL), Set.of(Process.FUEL));

    public static Profile profile(net.minecraft.world.item.ItemStack stack) {
        var material = br.com.quantumindustries.assay.AssayCatalog.material(stack);
        if (material == Material.COAL) return COAL;
        if (material != null) return METALS.get(material);
        if (stack.is(net.minecraft.tags.ItemTags.LOGS) || stack.is(net.minecraft.tags.ItemTags.PLANKS)) return WOOD;
        if (stack.is(net.minecraft.world.item.Items.STONE) || stack.is(net.minecraft.world.item.Items.COBBLESTONE)) return STONE;
        return null;
    }

    public static Map<Material, Profile> metals() { return METALS; }
    public static boolean supports(Material material, Process process) {
        if (material == null) return false;
        Profile profile = METALS.get(material);
        return profile != null && profile.processes().contains(process);
    }
}
