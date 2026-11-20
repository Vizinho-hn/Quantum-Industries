package br.com.quantumindustries.research;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.research.ResearchCatalog.Study;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GuidebookCatalog {

    public record Chapter(String id, String studyKey, int era, String title, String subtitle,
                          Supplier<ItemStack> icon, List<String> paragraphs, String operationSummary) {
        public Identifier identifier() { return ResearchIds.of(id); }
    }

    public static final Chapter INTRO_VAULT = new Chapter(
        "intro_vault",
        null,
        0,
        "guidebook.quantumindustries.intro_vault.title",
        "guidebook.quantumindustries.intro_vault.subtitle",
        () -> new ItemStack(ModItems.SCIENTIFIC_ANALYZER_TIER_1.get()),
        List.of(
            "guidebook.quantumindustries.intro_vault.paragraph_0",
            "guidebook.quantumindustries.intro_vault.paragraph_1",
            "guidebook.quantumindustries.intro_vault.paragraph_2"
        ),
        "guidebook.quantumindustries.intro_vault.operation"
    );

    public static final List<Chapter> CHAPTERS = List.of(
        INTRO_VAULT,
        new Chapter("material_observation", "material_observation", 1,
            "guidebook.quantumindustries.material_observation.title",
            "guidebook.quantumindustries.material_observation.subtitle",
            () -> new ItemStack(ModItems.EMPIRICAL_ASSAY_BENCH.get()),
            List.of(
                "guidebook.quantumindustries.material_observation.paragraph_0",
                "guidebook.quantumindustries.material_observation.paragraph_1",
                "guidebook.quantumindustries.material_observation.paragraph_2"
            ),
            "guidebook.quantumindustries.material_observation.operation"
        ),
        new Chapter("manual_forming", "manual_forming", 1,
            "guidebook.quantumindustries.manual_forming.title",
            "guidebook.quantumindustries.manual_forming.subtitle",
            () -> new ItemStack(ModItems.FORMING_BENCH.get()),
            List.of(
                "guidebook.quantumindustries.manual_forming.paragraph_0",
                "guidebook.quantumindustries.manual_forming.paragraph_1"
            ),
            "guidebook.quantumindustries.manual_forming.operation"
        ),
        new Chapter("alloy_metallurgy", "alloy_metallurgy", 1,
            "guidebook.quantumindustries.alloy_metallurgy.title",
            "guidebook.quantumindustries.alloy_metallurgy.subtitle",
            () -> new ItemStack(ModItems.BELLOWS_ALLOY_KILN.get()),
            List.of(
                "guidebook.quantumindustries.alloy_metallurgy.paragraph_0",
                "guidebook.quantumindustries.alloy_metallurgy.paragraph_1"
            ),
            "guidebook.quantumindustries.alloy_metallurgy.operation"
        ),
        new Chapter("mechanical_transmission", "mechanical_transmission", 1,
            "guidebook.quantumindustries.mechanical_transmission.title",
            "guidebook.quantumindustries.mechanical_transmission.subtitle",
            () -> new ItemStack(ModItems.MECHANICAL_SAWMILL.get()),
            List.of(
                "guidebook.quantumindustries.mechanical_transmission.paragraph_0",
                "guidebook.quantumindustries.mechanical_transmission.paragraph_1",
                "guidebook.quantumindustries.mechanical_transmission.paragraph_2",
                "guidebook.quantumindustries.mechanical_transmission.paragraph_3",
                "guidebook.quantumindustries.mechanical_transmission.paragraph_4"
            ),
            "guidebook.quantumindustries.mechanical_transmission.operation"
        ),
        // Optional physics lesson: unlocks with existing Mechanical Transmission research.
        // No new research node, crafting gate, or progression prerequisite.
        new Chapter("rotation_principles", "mechanical_transmission", 1,
            "guidebook.quantumindustries.rotation_principles.title",
            "guidebook.quantumindustries.rotation_principles.subtitle",
            () -> new ItemStack(ModItems.MECHANICAL_SHAFT_BLOCK.get()),
            List.of(
                "guidebook.quantumindustries.rotation_principles.paragraph_0",
                "guidebook.quantumindustries.rotation_principles.paragraph_1",
                "guidebook.quantumindustries.rotation_principles.paragraph_2"
            ),
            "guidebook.quantumindustries.rotation_principles.operation"
        ),
        new Chapter("rotational_science", "mechanical_transmission", 1,
            "guidebook.quantumindustries.rotational_science.title",
            "guidebook.quantumindustries.rotational_science.subtitle",
            () -> new ItemStack(ModItems.IRON_FLYWHEEL.get()),
            List.of(
                "guidebook.quantumindustries.rotational_science.paragraph_0",
                "guidebook.quantumindustries.rotational_science.paragraph_1",
                "guidebook.quantumindustries.rotational_science.paragraph_2",
                "guidebook.quantumindustries.rotational_science.paragraph_3",
                "guidebook.quantumindustries.rotational_science.paragraph_4",
                "guidebook.quantumindustries.rotational_science.paragraph_5",
                "guidebook.quantumindustries.rotational_science.paragraph_6"
            ),
            "guidebook.quantumindustries.rotational_science.operation"
        ),
        new Chapter("water_moves_wheels", "hydraulic_power", 1,
            "guidebook.quantumindustries.water_moves_wheels.title",
            "guidebook.quantumindustries.water_moves_wheels.subtitle",
            () -> new ItemStack(ModItems.PRIMITIVE_WATER_WHEEL.get()),
            List.of("guidebook.quantumindustries.water_moves_wheels.paragraph_0",
                    "guidebook.quantumindustries.water_moves_wheels.paragraph_1",
                    "guidebook.quantumindustries.water_moves_wheels.paragraph_2",
                    "guidebook.quantumindustries.water_moves_wheels.paragraph_3"),
            "guidebook.quantumindustries.water_moves_wheels.operation"
        ),
        new Chapter("hydraulic_power", "hydraulic_power", 1,
            "guidebook.quantumindustries.hydraulic_power.title",
            "guidebook.quantumindustries.hydraulic_power.subtitle",
            () -> new ItemStack(ModItems.PRIMITIVE_WATER_WHEEL.get()),
            List.of(
                "guidebook.quantumindustries.hydraulic_power.paragraph_0",
                "guidebook.quantumindustries.hydraulic_power.paragraph_1",
                "guidebook.quantumindustries.hydraulic_power.paragraph_2"
            ),
            "guidebook.quantumindustries.hydraulic_power.operation"
        ),
        new Chapter("mineral_beneficiation", "mineral_beneficiation", 1,
            "guidebook.quantumindustries.mineral_beneficiation.title",
            "guidebook.quantumindustries.mineral_beneficiation.subtitle",
            () -> new ItemStack(ModItems.JAW_CRUSHER.get()),
            List.of(
                "guidebook.quantumindustries.mineral_beneficiation.paragraph_0",
                "guidebook.quantumindustries.mineral_beneficiation.paragraph_1"
            ),
            "guidebook.quantumindustries.mineral_beneficiation.operation"
        ),
        new Chapter("copper_wire", "copper_wire", 1,
            "guidebook.quantumindustries.copper_wire.title",
            "guidebook.quantumindustries.copper_wire.subtitle",
            () -> new ItemStack(ModItems.COPPER_WIRE.get()),
            List.of(
                "guidebook.quantumindustries.copper_wire.paragraph_0",
                "guidebook.quantumindustries.copper_wire.paragraph_1"
            ),
            "guidebook.quantumindustries.copper_wire.operation"
        ),
        new Chapter("manual_dynamo_design", "manual_dynamo_design", 1,
            "guidebook.quantumindustries.manual_dynamo_design.title",
            "guidebook.quantumindustries.manual_dynamo_design.subtitle",
            () -> new ItemStack(ModItems.MANUAL_DYNAMO.get()),
            List.of(
                "guidebook.quantumindustries.manual_dynamo_design.paragraph_0",
                "guidebook.quantumindustries.manual_dynamo_design.paragraph_1"
            ),
            "guidebook.quantumindustries.manual_dynamo_design.operation"
        ),
        new Chapter("scientific_analyzer_tier_2", "scientific_analyzer_tier_2", 1,
            "guidebook.quantumindustries.scientific_analyzer_tier_2.title",
            "guidebook.quantumindustries.scientific_analyzer_tier_2.subtitle",
            () -> new ItemStack(ModItems.SCIENTIFIC_ANALYZER.get()),
            List.of(
                "guidebook.quantumindustries.scientific_analyzer_tier_2.paragraph_0",
                "guidebook.quantumindustries.scientific_analyzer_tier_2.paragraph_1",
                "guidebook.quantumindustries.scientific_analyzer_tier_2.paragraph_2"
            ),
            "guidebook.quantumindustries.scientific_analyzer_tier_2.operation"
        ),
        new Chapter("galvanic_storage", "galvanic_storage", 2,
            "guidebook.quantumindustries.galvanic_storage.title",
            "guidebook.quantumindustries.galvanic_storage.subtitle",
            () -> new ItemStack(ModItems.LEAD_ACID_ACCUMULATOR.get()),
            List.of(
                "guidebook.quantumindustries.galvanic_storage.paragraph_0",
                "guidebook.quantumindustries.galvanic_storage.paragraph_1"
            ),
            "guidebook.quantumindustries.galvanic_storage.operation"
        ),
        new Chapter("load_protection", "load_protection", 2,
            "guidebook.quantumindustries.load_protection.title",
            "guidebook.quantumindustries.load_protection.subtitle",
            () -> new ItemStack(ModItems.LOAD_RELAY.get()),
            List.of(
                "guidebook.quantumindustries.load_protection.paragraph_0",
                "guidebook.quantumindustries.load_protection.paragraph_1"
            ),
            "guidebook.quantumindustries.load_protection.operation"
        ),
        new Chapter("thermal_annealing", "thermal_annealing", 2,
            "guidebook.quantumindustries.thermal_annealing.title",
            "guidebook.quantumindustries.thermal_annealing.subtitle",
            () -> new ItemStack(ModItems.BELLOWS_BRAZIER.get()),
            List.of(
                "guidebook.quantumindustries.thermal_annealing.paragraph_0",
                "guidebook.quantumindustries.thermal_annealing.paragraph_1"
            ),
            "guidebook.quantumindustries.thermal_annealing.operation"
        ),
        new Chapter("wire_drawing", "wire_drawing", 2,
            "guidebook.quantumindustries.wire_drawing.title",
            "guidebook.quantumindustries.wire_drawing.subtitle",
            () -> new ItemStack(ModItems.WIRE_DRAWING_BENCH.get()),
            List.of(
                "guidebook.quantumindustries.wire_drawing.paragraph_0",
                "guidebook.quantumindustries.wire_drawing.paragraph_1"
            ),
            "guidebook.quantumindustries.wire_drawing.operation"
        ),
        new Chapter("electromechanical_forming", "electromechanical_forming", 2,
            "guidebook.quantumindustries.electromechanical_forming.title",
            "guidebook.quantumindustries.electromechanical_forming.subtitle",
            () -> new ItemStack(ModItems.ELECTROMECHANICAL_PRESS.get()),
            List.of(
                "guidebook.quantumindustries.electromechanical_forming.paragraph_0",
                "guidebook.quantumindustries.electromechanical_forming.paragraph_1"
            ),
            "guidebook.quantumindustries.electromechanical_forming.operation"
        ),
        new Chapter("electric_alloy_smelting", "electric_alloy_smelting", 2,
            "guidebook.quantumindustries.electric_alloy_smelting.title",
            "guidebook.quantumindustries.electric_alloy_smelting.subtitle",
            () -> new ItemStack(ModItems.ELECTRIC_ALLOY_SMELTER.get()),
            List.of(
                "guidebook.quantumindustries.electric_alloy_smelting.paragraph_0",
                "guidebook.quantumindustries.electric_alloy_smelting.paragraph_1"
            ),
            "guidebook.quantumindustries.electric_alloy_smelting.operation"
        ),
        new Chapter("fluid_hydrostatics", "fluid_hydrostatics", 2,
            "guidebook.quantumindustries.fluid_hydrostatics.title",
            "guidebook.quantumindustries.fluid_hydrostatics.subtitle",
            () -> new ItemStack(ModItems.SETTLING_VAT.get()),
            List.of(
                "guidebook.quantumindustries.fluid_hydrostatics.paragraph_0",
                "guidebook.quantumindustries.fluid_hydrostatics.paragraph_1"
            ),
            "guidebook.quantumindustries.fluid_hydrostatics.operation"
        ),
        new Chapter("pneumatic_transport", "pneumatic_transport", 2,
            "guidebook.quantumindustries.pneumatic_transport.title",
            "guidebook.quantumindustries.pneumatic_transport.subtitle",
            () -> new ItemStack(ModItems.PNEUMATIC_TUBE.get()),
            List.of(
                "guidebook.quantumindustries.pneumatic_transport.paragraph_0",
                "guidebook.quantumindustries.pneumatic_transport.paragraph_1"
            ),
            "guidebook.quantumindustries.pneumatic_transport.operation"
        ),
        new Chapter("chemical_leaching", "chemical_leaching", 2,
            "guidebook.quantumindustries.chemical_leaching.title",
            "guidebook.quantumindustries.chemical_leaching.subtitle",
            () -> new ItemStack(ModItems.PHOSPHATE_FERTILIZER.get()),
            List.of(
                "guidebook.quantumindustries.chemical_leaching.paragraph_0",
                "guidebook.quantumindustries.chemical_leaching.paragraph_1"
            ),
            "guidebook.quantumindustries.chemical_leaching.operation"
        )
    );

    static {
        for (Chapter chapter : CHAPTERS) {
            if (chapter.studyKey() != null) {
                Study study = ResearchCatalog.find(ResearchCatalog.identifier(chapter.studyKey()));
                if (study == null) {
                    throw new IllegalStateException("Study key '" + chapter.studyKey() + "' not found in ResearchCatalog");
                }
            }
        }
    }

    private GuidebookCatalog() {}

    public static Chapter introVault() { return INTRO_VAULT; }
    public static List<Chapter> chapters() { return CHAPTERS; }
}
