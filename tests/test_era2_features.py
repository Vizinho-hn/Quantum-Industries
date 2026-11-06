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
)
ITEMS = ("empirical_field_notes", "heated_iron_ingot",
         "heated_copper_ingot", "gold_sheet", "gold_wire")
RESEARCH = ("wire_drawing", "thermal_annealing", "piezoelectricity",
            "load_protection")


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
            self.assertIn("item.quantumindustries.empirical_field_notes", labels)

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


if __name__ == "__main__":
    unittest.main()
