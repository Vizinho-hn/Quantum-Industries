package br.com.quantumindustries.research;

import java.util.List;
import java.util.function.Supplier;
import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.assay.AssayCatalog.Material;
import br.com.quantumindustries.assay.AssayCatalog.Method;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ResearchCatalog {
    public record Cost(Supplier<? extends Item> itemSupplier, int amount, String label) {
        public Cost(Item item, int amount, String label) {
            this(() -> item, amount, label);
        }
        public Item item() {
            return itemSupplier.get();
        }
    }
    public record Study(String key, String name, String description, String reward, Item icon,
                        int era, List<String> prerequisites, List<Cost> costs, List<ResearchEvidence> evidence) {
        public Identifier id() { return ResearchIds.of(key); }
    }
    // Ordering is for display and transient menu messages only.
    public static final List<Study> STUDIES = List.of(
        new Study("conductivity", "research.quantumindustries.conductivity.name", "research.quantumindustries.conductivity.description", "research.quantumindustries.conductivity.reward", Items.REDSTONE, 1, List.of("material_observation"), List.of(new Cost(Items.COPPER_INGOT, 3, "lingotes de cobre"), new Cost(Items.REDSTONE, 1, "redstone"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.Assay(Material.COPPER, Method.INSPECTION))),
        new Study("copper_wire", "research.quantumindustries.copper_wire.name", "research.quantumindustries.copper_wire.description", "research.quantumindustries.copper_wire.reward", Items.COPPER_INGOT, 1, List.of("conductivity"), List.of(new Cost(Items.COPPER_INGOT, 2, "lingotes de cobre"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.Assay(Material.COPPER, Method.MECHANICAL))),
        new Study("material_observation", "research.quantumindustries.material_observation.name", "research.quantumindustries.material_observation.description", "research.quantumindustries.material_observation.reward", Items.SPYGLASS, 1, List.of(), List.of(new Cost(Items.OAK_PLANKS, 1, "tábua"), new Cost(Items.COBBLESTONE, 1, "pedregulho"), new Cost(Items.COPPER_INGOT, 1, "lingote de cobre")), List.of()),
        new Study("material_malleability", "research.quantumindustries.material_malleability.name", "research.quantumindustries.material_malleability.description", "research.quantumindustries.material_malleability.reward", Items.COPPER_INGOT, 1, List.of("material_observation"), List.of(new Cost(Items.COPPER_INGOT, 1, "lingote de cobre"), new Cost(Items.STONE, 1, "pedra")), List.of(new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.COPPER, Method.MECHANICAL), new ResearchEvidence.Assay(Material.GOLD, Method.MECHANICAL), new ResearchEvidence.Assay(Material.IRON, Method.MECHANICAL))))),
        new Study("manual_forming", "research.quantumindustries.manual_forming.name", "research.quantumindustries.manual_forming.description", "research.quantumindustries.manual_forming.reward", Items.IRON_NUGGET, 1, List.of("material_malleability"), List.of(new Cost(Items.IRON_NUGGET, 3, "pepitas de ferro"), new Cost(Items.STICK, 2, "gravetos")), List.of()),
        new Study("metal_sheets", "research.quantumindustries.metal_sheets.name", "research.quantumindustries.metal_sheets.description", "research.quantumindustries.metal_sheets.reward", Items.IRON_INGOT, 1, List.of("manual_forming"), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro"), new Cost(Items.COPPER_INGOT, 1, "lingote de cobre")), List.of(new ResearchEvidence.Assay(Material.IRON, Method.MECHANICAL))),
        new Study("electrical_insulation", "research.quantumindustries.electrical_insulation.name", "research.quantumindustries.electrical_insulation.description", "research.quantumindustries.electrical_insulation.reward", Items.LEATHER, 1, List.of("copper_wire"), List.of(new Cost(Items.LEATHER, 1, "couro"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.COAL, Method.INSPECTION), new ResearchEvidence.Assay(Material.QUARTZ, Method.INSPECTION))))),
        new Study("basic_mechanics", "research.quantumindustries.basic_mechanics.name", "research.quantumindustries.basic_mechanics.description", "research.quantumindustries.basic_mechanics.reward", Items.IRON_INGOT, 1, List.of("manual_forming"), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro")), List.of(new ResearchEvidence.Assay(Material.IRON, Method.MECHANICAL))),
        new Study("mechanical_transmission", "research.quantumindustries.mechanical_transmission.name", "research.quantumindustries.mechanical_transmission.description", "research.quantumindustries.mechanical_transmission.reward", Items.STICK, 1, List.of("basic_mechanics"), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro"), new Cost(Items.STICK, 1, "graveto")), List.of()),
        new Study("basic_magnetism", "research.quantumindustries.basic_magnetism.name", "research.quantumindustries.basic_magnetism.description", "research.quantumindustries.basic_magnetism.reward", Items.COMPASS, 1, List.of("material_observation"), List.of(new Cost(Items.COMPASS, 1, "bússola")), List.of(new ResearchEvidence.Assay(Material.IRON, Method.INSPECTION))),
        new Study("copper_winding", "research.quantumindustries.copper_winding.name", "research.quantumindustries.copper_winding.description", "research.quantumindustries.copper_winding.reward", Items.COPPER_INGOT, 1, List.of("copper_wire"), List.of(new Cost(Items.STICK, 1, "graveto"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.Assay(Material.COPPER, Method.MECHANICAL))),
        new Study("electromechanical_structure", "research.quantumindustries.electromechanical_structure.name", "research.quantumindustries.electromechanical_structure.description", "research.quantumindustries.electromechanical_structure.reward", Items.IRON_INGOT, 1, List.of("metal_sheets", "mechanical_transmission", "basic_magnetism", "copper_winding"), List.of(new Cost(Items.IRON_INGOT, 1, "lingote de ferro")), List.of(new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.IRON, Method.HEATING), new ResearchEvidence.Assay(Material.COPPER, Method.HEATING))))),
        new Study("manual_dynamo_design", "research.quantumindustries.manual_dynamo_design.name", "research.quantumindustries.manual_dynamo_design.description", "research.quantumindustries.manual_dynamo_design.reward", Items.BOOK, 1, List.of("electrical_insulation", "electromechanical_structure"), List.of(new Cost(Items.PAPER, 1, "papel")), List.of()),
        new Study("electromagnetic_induction", "research.quantumindustries.electromagnetic_induction.name", "research.quantumindustries.electromagnetic_induction.description", "research.quantumindustries.electromagnetic_induction.reward", Items.REDSTONE, 2, List.of("manual_dynamo_design"), List.of(new Cost(Items.REDSTONE, 1, "redstone"), new Cost(Items.IRON_INGOT, 1, "lingote de ferro")), List.of(new ResearchEvidence.Assay(Material.REDSTONE, Method.REACTION), new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.COPPER, Method.REACTION), new ResearchEvidence.Assay(Material.LAPIS, Method.REACTION))))),
        new Study("galvanic_storage", "research.quantumindustries.galvanic_storage.name", "research.quantumindustries.galvanic_storage.description", "research.quantumindustries.galvanic_storage.reward", Items.LAPIS_LAZULI, 2, List.of("electromagnetic_induction"), List.of(new Cost(Items.LAPIS_LAZULI, 2, "lápis-lazúli"), new Cost(Items.COPPER_INGOT, 2, "lingotes de cobre"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.LEAD, Method.REACTION), new ResearchEvidence.Assay(Material.LAPIS, Method.REACTION))), new ResearchEvidence.Assay(Material.COPPER, Method.REACTION))),
        new Study("fluid_hydrostatics", "research.quantumindustries.fluid_hydrostatics.name", "research.quantumindustries.fluid_hydrostatics.description", "research.quantumindustries.fluid_hydrostatics.reward", Items.WATER_BUCKET,
                2, List.of("metal_sheets"),
                List.of(new Cost(Items.COPPER_INGOT,2,"lingotes de cobre"),
                        new Cost(Items.GLASS,2,"vidros")),
                List.of(new ResearchEvidence.AnyOf(List.of(
                        new ResearchEvidence.Assay(Material.COPPER,Method.REACTION),
                        new ResearchEvidence.Assay(Material.LAPIS,Method.REACTION))))),
        new Study("pneumatic_transport", "research.quantumindustries.pneumatic_transport.name", "research.quantumindustries.pneumatic_transport.description", "research.quantumindustries.pneumatic_transport.reward",
                Items.GLASS,2,List.of("mechanical_transmission","metal_sheets"),
                List.of(new Cost(Items.COPPER_INGOT,2,"lingotes de cobre"),new Cost(Items.PAPER,1,"papel")),
                List.of(new ResearchEvidence.Assay(Material.COAL,Method.HEATING),
                        new ResearchEvidence.Assay(Material.QUARTZ,Method.INSPECTION))),
        new Study("peristaltic_mechanics", "research.quantumindustries.peristaltic_mechanics.name", "research.quantumindustries.peristaltic_mechanics.description", "research.quantumindustries.peristaltic_mechanics.reward",
                Items.PISTON,2,List.of("fluid_hydrostatics","electromechanical_structure"),
                List.of(new Cost(Items.IRON_INGOT,2,"lingotes de ferro"),
                        new Cost(Items.REDSTONE,1,"redstone")),
                List.of(new ResearchEvidence.Assay(Material.IRON,Method.MECHANICAL))),
        new Study("piezoelectricity", "research.quantumindustries.piezoelectricity.name", "research.quantumindustries.piezoelectricity.description", "research.quantumindustries.piezoelectricity.reward",
                Items.QUARTZ, 2, List.of("electromagnetic_induction"),
                List.of(new Cost(Items.QUARTZ, 2, "quartzos"), new Cost(Items.REDSTONE, 1, "redstone")),
                List.of(new ResearchEvidence.Assay(Material.QUARTZ, Method.MECHANICAL),
                        new ResearchEvidence.Assay(Material.REDSTONE, Method.REACTION))),
        new Study("load_protection", "research.quantumindustries.load_protection.name", "research.quantumindustries.load_protection.description", "research.quantumindustries.load_protection.reward",
                Items.COMPARATOR, 2, List.of("galvanic_storage"),
                List.of(new Cost(Items.QUARTZ, 1, "quartzo"), new Cost(Items.REDSTONE, 2, "redstone")),
                List.of(new ResearchEvidence.Assay(Material.QUARTZ, Method.INSPECTION),
                        new ResearchEvidence.Assay(Material.REDSTONE, Method.REACTION))),
        new Study("thermal_annealing", "research.quantumindustries.thermal_annealing.name", "research.quantumindustries.thermal_annealing.description", "research.quantumindustries.thermal_annealing.reward",
                Items.COAL, 2, List.of("metal_sheets"),
                List.of(new Cost(Items.COAL, 2, "carvão"), new Cost(Items.QUARTZ, 1, "quartzo")),
                List.of(new ResearchEvidence.Assay(Material.COAL, Method.HEATING),
                        new ResearchEvidence.AnyOf(List.of(new ResearchEvidence.Assay(Material.IRON, Method.HEATING),
                                new ResearchEvidence.Assay(Material.COPPER, Method.HEATING))))),
        new Study("wire_drawing", "research.quantumindustries.wire_drawing.name", "research.quantumindustries.wire_drawing.description", "research.quantumindustries.wire_drawing.reward",
                Items.GOLD_INGOT, 2, List.of("electromechanical_structure", "metal_sheets"),
                List.of(new Cost(Items.GOLD_INGOT, 1, "lingote de ouro"),
                        new Cost(Items.COPPER_INGOT, 1, "lingote de cobre")),
                List.of(new ResearchEvidence.Assay(Material.COPPER, Method.MECHANICAL),
                        new ResearchEvidence.Assay(Material.GOLD, Method.MECHANICAL))),
        new Study("electromechanical_forming", "research.quantumindustries.electromechanical_forming.name", "research.quantumindustries.electromechanical_forming.description", "research.quantumindustries.electromechanical_forming.reward", Items.ANVIL, 2, List.of("electromagnetic_induction"), List.of(new Cost(Items.IRON_INGOT, 2, "lingotes de ferro"), new Cost(Items.REDSTONE, 1, "redstone"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.Assay(Material.IRON, Method.MECHANICAL), new ResearchEvidence.Assay(Material.REDSTONE, Method.REACTION))),
        new Study("hydraulic_power", "research.quantumindustries.hydraulic_power.name", "research.quantumindustries.hydraulic_power.description", "research.quantumindustries.hydraulic_power.reward", Items.OAK_LOG, 1, List.of("mechanical_transmission"), List.of(new Cost(Items.OAK_PLANKS, 2, "tábuas"), new Cost(Items.IRON_INGOT, 1, "lingote de ferro"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.Assay(Material.IRON, Method.MECHANICAL))),
        new Study("alloy_metallurgy", "research.quantumindustries.alloy_metallurgy.name", "research.quantumindustries.alloy_metallurgy.description", "research.quantumindustries.alloy_metallurgy.reward",
                Items.COPPER_INGOT, 1, List.of("metal_sheets"),
                List.of(new Cost(Items.COPPER_INGOT, 2, "lingotes de cobre"),
                        new Cost(Items.COAL, 2, "carvão")),
                List.of(new ResearchEvidence.Assay(Material.COPPER, Method.HEATING),
                        new ResearchEvidence.AnyOf(List.of(
                                new ResearchEvidence.Assay(Material.TIN, Method.HEATING),
                                new ResearchEvidence.Assay(Material.ZINC, Method.HEATING))))),
        new Study("electric_alloy_smelting", "research.quantumindustries.electric_alloy_smelting.name", "research.quantumindustries.electric_alloy_smelting.description", "research.quantumindustries.electric_alloy_smelting.reward",
                Items.BLAST_FURNACE, 2, List.of("electromagnetic_induction", "alloy_metallurgy"),
                List.of(new Cost(Items.IRON_INGOT, 2, "lingotes de ferro"),
                        new Cost(Items.REDSTONE, 2, "redstone"),
                        new Cost(Items.PAPER, 1, "papel")),
                List.of(new ResearchEvidence.Assay(Material.IRON, Method.HEATING),
                        new ResearchEvidence.Assay(Material.REDSTONE, Method.REACTION))),
        new Study("mineral_beneficiation", "research.quantumindustries.mineral_beneficiation.name", "research.quantumindustries.mineral_beneficiation.description", "research.quantumindustries.mineral_beneficiation.reward",
                Items.GRINDSTONE,
                1, List.of("mechanical_transmission", "metal_sheets"),
                List.of(new Cost(Items.IRON_INGOT, 2, "lingotes de ferro"),
                        new Cost(Items.COBBLESTONE, 4, "pedregulhos"),
                        new Cost(Items.PAPER, 1, "papel")),
                List.of(new ResearchEvidence.Assay(Material.IRON, Method.MECHANICAL),
                        new ResearchEvidence.AnyOf(List.of(
                                new ResearchEvidence.Assay(Material.TIN, Method.MECHANICAL),
                                new ResearchEvidence.Assay(Material.ZINC, Method.MECHANICAL),
                                new ResearchEvidence.Assay(Material.LEAD, Method.MECHANICAL))))),
        new Study("scientific_analyzer_tier_2", "research.quantumindustries.scientific_analyzer_tier_2.name", "research.quantumindustries.scientific_analyzer_tier_2.description", "research.quantumindustries.scientific_analyzer_tier_2.reward",
                Items.BOOK,
                1, List.of("manual_dynamo_design", "mineral_beneficiation"),
                List.of(new Cost(Items.DEEPSLATE, 2, "deepslate"),
                        new Cost(Items.LAPIS_LAZULI, 2, "lápis-lazúli"),
                        new Cost(Items.COPPER_INGOT, 2, "lingotes de cobre"),
                        new Cost(Items.PAPER, 1, "papel")),
                List.of()),
        new Study("chemical_leaching", "research.quantumindustries.chemical_leaching.name", "research.quantumindustries.chemical_leaching.description", "research.quantumindustries.chemical_leaching.reward",
                Items.GLASS_BOTTLE,
                2, List.of("fluid_hydrostatics"),
                List.of(new Cost(Items.GLASS_BOTTLE, 2, "frascos de vidro"),
                        new Cost(Items.LAPIS_LAZULI, 2, "lápis-lazúli"),
                        new Cost(Items.PAPER, 1, "papel")),
                List.of(new ResearchEvidence.AnyOf(List.of(
                                new ResearchEvidence.Assay(Material.LEAD, Method.REACTION),
                                new ResearchEvidence.Assay(Material.ZINC, Method.REACTION),
                                new ResearchEvidence.Assay(Material.LAPIS, Method.REACTION))))),
        new Study("electric_wood_processing", "research.quantumindustries.electric_wood_processing.name", "research.quantumindustries.electric_wood_processing.description", "research.quantumindustries.electric_wood_processing.reward", Items.IRON_AXE, 2, List.of("mechanical_transmission", "electromagnetic_induction", "electromechanical_forming"), List.of(new Cost(Items.IRON_INGOT, 2, "lingotes de ferro"), new Cost(Items.COPPER_INGOT, 2, "lingotes de cobre")), List.of(new ResearchEvidence.Assay(Material.IRON, Method.MECHANICAL), new ResearchEvidence.Assay(Material.COPPER, Method.REACTION))),
        new Study("electromagnetic_excitation", "research.quantumindustries.electromagnetic_excitation.name", "research.quantumindustries.electromagnetic_excitation.description", "research.quantumindustries.electromagnetic_excitation.reward", Items.COMPASS, 2, List.of("electromagnetic_induction", "galvanic_storage"), List.of(new Cost(Items.IRON_INGOT, 2, "lingotes de ferro"), new Cost(Items.COPPER_INGOT, 2, "lingotes de cobre")), List.of()),
        new Study("alternating_current", "research.quantumindustries.alternating_current.name", "research.quantumindustries.alternating_current.description", "research.quantumindustries.alternating_current.reward", Items.REDSTONE, 2, List.of("electromagnetic_excitation"), List.of(new Cost(Items.REDSTONE, 2, "redstone"), new Cost(Items.QUARTZ, 1, "quartzo")), List.of()),
        new Study("industrial_generation", "research.quantumindustries.industrial_generation.name", "research.quantumindustries.industrial_generation.description", "research.quantumindustries.industrial_generation.reward", Items.COPPER_INGOT, 2, List.of("alternating_current", "electromechanical_forming"), List.of(new Cost(Items.IRON_INGOT, 2, "lingotes de ferro"), new Cost(Items.PAPER, 1, "papel")), List.of(new ResearchEvidence.Instrumental("copper"), new ResearchEvidence.Instrumental("iron"))),
        new Study("mechanical_energy_storage", "research.quantumindustries.mechanical_energy_storage.name", "research.quantumindustries.mechanical_energy_storage.description", "research.quantumindustries.mechanical_energy_storage.reward", Items.IRON_BLOCK, 1, List.of("mechanical_transmission", "metal_sheets"), List.of(new Cost(Items.IRON_INGOT, 2, "lingotes de ferro"), new Cost(Items.PAPER, 1, "papel")), List.of())
    );

    public record Branch(String title, List<String> studies, String knowledge, String application) {}

    public static final List<Branch> ERA_ONE_BRANCHES = List.of(
            new Branch("gui.quantumindustries.text.materials_science", List.of("material_observation", "material_malleability", "manual_forming", "metal_sheets", "alloy_metallurgy"),
                    "gui.quantumindustries.text.malleable_metals_can_be_shaped_with_controlled_blows_and_smelted_into_alloys",
                    "gui.quantumindustries.text.use_the_hammer_forming_bench_and_bellows_alloy_kiln_to_produce_sheets_bronze_and_brass"),
            new Branch("gui.quantumindustries.text.mechanical_engineering", List.of("basic_mechanics", "mechanical_transmission", "mineral_beneficiation"),
                    "gui.quantumindustries.text.gears_and_shafts_transmit_motion_manual_or_hydraulic_sources_can_drive_the_workshop_and_crush_ores",
                    "gui.quantumindustries.text.crank_the_sawmill_or_jaw_crusher_or_transmit_motion_from_a_water_wheel_through_shafts"),
            new Branch("gui.quantumindustries.text.basic_electricity", List.of("conductivity", "copper_wire", "electrical_insulation", "copper_winding"),
                    "gui.quantumindustries.text.copper_conducts_electricity_a_coating_insulates_the_wire_and_winding_forms_a_coil",
                    "gui.quantumindustries.text.prepare_insulated_wires_and_coils_for_your_first_electrical_projects")
    );

    public static String sectionTitle(int study) {
        if (STUDIES.get(study).key().equals("hydraulic_power")) return "gui.quantumindustries.text.mechanical_engineering";
        if (STUDIES.get(study).key().equals("basic_magnetism")) return "gui.quantumindustries.text.basic_electricity";
        for (Branch branch : ERA_ONE_BRANCHES) {
            if (branch.studies().contains(STUDIES.get(study).key())) return branch.title();
        }
        return STUDIES.get(study).era() == 1 ? "gui.quantumindustries.text.electromechanical_integration" : "gui.quantumindustries.text.era_ii_electrical_revolution";
    }

    private static final java.util.Map<String, Study> BY_KEY = STUDIES.stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(Study::key, study -> study));
    private static final java.util.Map<Identifier, Study> BY_ID = STUDIES.stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(Study::id, study -> study));

    static {
        var visited = new java.util.HashSet<String>();
        var visiting = new java.util.HashSet<String>();
        for (String key : BY_KEY.keySet()) validate(key, visited, visiting);
    }

    private static void validate(String key, java.util.Set<String> visited, java.util.Set<String> visiting) {
        if (visited.contains(key)) return;
        Study study = BY_KEY.get(key);
        if (study == null) throw new IllegalStateException("Unknown research prerequisite: " + key);
        if (study.evidence().size() > 16) throw new IllegalStateException("Too many evidence requirements: " + key);
        if (!visiting.add(key)) throw new IllegalStateException("Cyclic research prerequisite: " + key);
        for (String dependency : study.prerequisites()) validate(dependency, visited, visiting);
        visiting.remove(key);
        visited.add(key);
    }

    /** An era groups studies for presentation, but does not grant or block knowledge. */
    public static boolean hasStudiesInEra(int era) {
        return STUDIES.stream().anyMatch(study -> study.era() == era);
    }

    public static Identifier identifier(String value) {
        return ResearchIds.parse(value);
    }

    public static Study find(Identifier id) { return id == null ? null : BY_ID.get(id); }

    public static Study find(String key) { return find(identifier(key)); }

    public static int indexOf(String key) { return STUDIES.indexOf(find(key)); }

    private ResearchCatalog() {}

    public static boolean valid(int id) { return id >= 0 && id < STUDIES.size(); }
}
