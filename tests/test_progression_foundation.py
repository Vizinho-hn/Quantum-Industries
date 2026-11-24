"""Focused source contracts for the Era I manual metallurgy bootstrap.

These checks are not a replacement for the dedicated-server survival suite.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/br/com/quantumindustries"


class ProgressionFoundationTest(unittest.TestCase):
    def test_bootstrap_crafts(self):
        source = (JAVA / "recipe/FoundationRecipe.java").read_text(encoding="utf-8")
        def pattern(item):
            match = re.search(r'new Pattern\("[^"]+", ModItems\.' + item + r'\b[\s\S]*?\),\n', source)
            self.assertIsNotNone(match, item)
            return match.group()
        kiln = pattern("BELLOWS_ALLOY_KILN")
        self.assertIn("Items.CAMPFIRE", kiln)
        self.assertNotIn("BELLOWS_BRAZIER", kiln)
        crusher = pattern("JAW_CRUSHER")
        for forbidden in ("LOAD_RELAY", "ELECTROMECHANICAL_FRAME", "BRONZE_GEAR", "insulated"):
            self.assertNotIn(forbidden, crusher)

    def test_alloy_catalog_and_manual_access(self):
        catalog = (JAVA / "material/AlloyCatalog.java").read_text(encoding="utf-8")
        recipes = re.findall(r'new Recipe\("([^"]+)", ([12]),', catalog)
        self.assertEqual([name for name, tier in recipes],
                         ["bronze", "brass", "invar", "electrum", "constantan", "steel"])
        self.assertIn("MANUAL_MAX_TIER = 2", catalog)
        kiln = (JAVA / "block/entity/BellowsAlloyKilnBlockEntity.java").read_text(encoding="utf-8")
        block = (JAVA / "block/BellowsAlloyKilnBlock.java").read_text(encoding="utf-8")
        self.assertIn("AlloyCatalog.MANUAL_MAX_TIER", kiln)
        self.assertIn("AlloyCatalog.MANUAL_MAX_TIER", block)
        self.assertIn("recipe.workTicks() * 2", kiln)
        self.assertIn('equals("steel") ? 240 : 160', kiln)
        self.assertIn("requiredTicks(recipe) - 1", kiln)
        self.assertNotIn("fuel += FUEL_PER_COAL / 2", kiln)
        self.assertIn('out.putInt("flux_reserved"', kiln)
        jei = (ROOT / "src/client/java/br/com/quantumindustries/client/jei/QuantumJeiPlugin.java").read_text(encoding="utf-8")
        self.assertNotIn("recipe.tier() == 1", jei)


if __name__ == "__main__":
    unittest.main()
