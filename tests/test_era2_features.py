"""Static resource contracts for Era II extensions.

Runtime energy correctness is verified separately in Minecraft/NeoForge.
"""
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"
DATA = ROOT / "src/main/resources/data/quantumindustries"
JAVA = ROOT / "src/main/java/br/com/quantumindustries"

BLOCKS = (
    "copper_busbar", "insulated_conduit", "wire_drawing_bench",
    "bellows_brazier", "piezo_switch", "load_relay",
    "bellows_alloy_kiln", "electric_alloy_smelter", "jaw_crusher",
)
ITEMS = ("empirical_field_notes", "heated_iron_ingot",
         "heated_copper_ingot", "heated_bronze_ingot", "heated_brass_ingot", "heated_steel_ingot",
         "gold_sheet", "gold_wire", "electrum_wire", "constantan_wire", "constantan_coil",
         "hydrochloric_acid", "refractory_brick", "phosphate_fertilizer",
         "lead_sheet", "tin_sheet", "zinc_sheet", "bronze_gear",
         "copper_dust", "iron_dust", "gold_dust", "tin_dust",
         "zinc_dust", "lead_dust", "nickel_dust", "silver_dust",
         "bronze_ingot", "bronze_nugget", "bronze_sheet",
         "brass_ingot", "brass_nugget", "brass_sheet",
         "invar_ingot", "invar_nugget", "invar_sheet",
         "electrum_ingot", "electrum_nugget", "electrum_sheet",
         "constantan_ingot", "constantan_nugget", "constantan_sheet",
         "steel_ingot", "steel_nugget", "steel_sheet")
RESEARCH = ("wire_drawing", "thermal_annealing", "piezoelectricity",
            "load_protection", "alloy_metallurgy", "electric_alloy_smelting",
            "mineral_beneficiation", "chemical_leaching")


class Era2Contracts(unittest.TestCase):
    def test_every_block_has_all_resources(self):
        for name in BLOCKS:
            with self.subTest(block=name):
                for folder in ("blockstates", "items", "models/block"):
                    resource = ASSETS / folder / f"{name}.json"
                    self.assertTrue(resource.is_file(), resource)
                    json.loads(resource.read_text(encoding="utf-8"))
                recipe = DATA / "recipe" / f"{name}.json"
                self.assertEqual(json.loads(recipe.read_text(encoding="utf-8"))["craft"], name)
                self.assertTrue((DATA / "loot_table/blocks" / f"{name}.json").is_file())

    def test_registered_items_and_localizations(self):
        for name in ITEMS:
            with self.subTest(item=name):
                for folder in ("items", "models/item"):
                    json.loads((ASSETS / folder / f"{name}.json").read_text(encoding="utf-8"))
        for language in ("pt_br", "en_us"):
            labels = json.loads((ASSETS / "lang" / f"{language}.json").read_text(encoding="utf-8"))
            for name in BLOCKS:
                self.assertIn(f"block.quantumindustries.{name}", labels)
            for name in ITEMS:
                self.assertIn(f"item.quantumindustries.{name}", labels)

    def test_research_and_crafts_are_bound(self):
        research = (JAVA / "research/ResearchCatalog.java").read_text(encoding="utf-8")
        patterns = (JAVA / "recipe/FoundationRecipe.java").read_text(encoding="utf-8")
        for name in RESEARCH:
            self.assertIn(f'new Study("{name}"', research)
        for name in BLOCKS:
            self.assertIn(f'ModItems.{name.upper()}', patterns)

    def test_energy_conservation_and_sharing_guards_present(self):
        network = (JAVA / "energy/CableNetwork.java").read_text(encoding="utf-8")
        self.assertIn("accepted + loss", network)
        self.assertIn("availableThisTick", network)
        notes = (JAVA / "assay/EmpiricalFieldNotesItem.java").read_text(encoding="utf-8")
        self.assertIn("learnComplete", notes)
        self.assertIn("stack.shrink(1)", notes)
        profile = (JAVA / "assay/AssayProfile.java").read_text(encoding="utf-8")
        self.assertIn("COMPLETE_MASK", profile)

    def test_alloy_smelters_and_lead_battery_chain(self):
        assay = (JAVA / "assay/AssayCatalog.java").read_text(encoding="utf-8")
        self.assertIn("MATERIAL_COUNT=13", assay)
        for mat in ("TIN", "ZINC", "LEAD"):
            self.assertIn(mat, assay)
        alloys = (JAVA / "material/AlloyCatalog.java").read_text(encoding="utf-8")
        for alloy in ("bronze", "brass", "invar", "electrum", "constantan", "steel"):
            self.assertIn(f'"{alloy}"', alloys)
        kiln_block = (JAVA / "block/BellowsAlloyKilnBlock.java").read_text(encoding="utf-8")
        self.assertIn('"alloy_metallurgy"', kiln_block)
        smelter_block = (JAVA / "block/ElectricAlloySmelterBlock.java").read_text(encoding="utf-8")
        self.assertIn("MachineProcessingMenu", smelter_block)
        processing_menu = (JAVA / "menu/MachineProcessingMenu.java").read_text(encoding="utf-8")
        self.assertIn('"electric_alloy_smelting"', processing_menu)
        smelter_entity = (JAVA / "block/entity/ElectricAlloySmelterBlockEntity.java").read_text(encoding="utf-8")
        self.assertIn("EnergyInspectable", smelter_entity)
        vat = (JAVA / "block/entity/SettlingVatBlockEntity.java").read_text(encoding="utf-8")
        self.assertIn('CHEMICAL_PRODUCTS.get("sulfur")', vat)
        patterns = (JAVA / "recipe/FoundationRecipe.java").read_text(encoding="utf-8")
        self.assertIn("ModItems.LEAD_SHEET.get()", patterns)
        self.assertIn("ModItems.GALVANIC_ELECTROLYTE.get()", patterns)

    def test_jaw_crusher_beneficiation_and_applied_chemistry(self):
        crusher_block = (JAVA / "block/JawCrusherBlock.java").read_text(encoding="utf-8")
        self.assertIn('"mineral_beneficiation"', crusher_block)
        crusher_entity = (JAVA / "block/entity/JawCrusherBlockEntity.java").read_text(encoding="utf-8")
        self.assertNotIn("EnergyInspectable", crusher_entity)
        self.assertNotIn("registerEnergy(event, ModBlocks.JAW_CRUSHER_ENTITY", (JAVA / "fluid/ModFluidCapabilities.java").read_text(encoding="utf-8"))
        crushing = (JAVA / "material/CrushingCatalog.java").read_text(encoding="utf-8")
        for metal in ("copper", "iron", "gold", "tin", "zinc", "lead", "nickel", "silver"):
            self.assertIn(f'"{metal}"', crushing)
        vat = (JAVA / "block/entity/SettlingVatBlockEntity.java").read_text(encoding="utf-8")
        self.assertIn("HYDROCHLORIC_ACID", vat)
        self.assertIn('CHEMICAL_PRODUCTS.get("halite")', vat)
        wire_bench = (JAVA / "block/entity/WireDrawingBenchBlockEntity.java").read_text(encoding="utf-8")
        self.assertIn("ELECTRUM_WIRE", wire_bench)
        self.assertIn("CONSTANTAN_WIRE", wire_bench)


if __name__ == "__main__":
    unittest.main()

