"""Guard industrial registration against accidental merge regressions."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/java/br/com/quantumindustries"

class IndustrialRegistryTest(unittest.TestCase):
    def test_industrial_blocks_items_and_entities(self):
        blocks = (ROOT / "core/ModBlocks.java").read_text(encoding="utf-8")
        items = (ROOT / "ModItems.java").read_text(encoding="utf-8")
        for name in ("ELECTROMAGNETIC_GENERATOR", "ELECTRIC_MOTOR", "STEAM_BOILER",
                     "STEAM_ENGINE", "THERMOELECTRIC_GENERATOR", "MECHANICAL_CONVEYOR",
                     "COMPACT_GEARBOX", "INDUSTRIAL_CIRCUIT_SWITCH"):
            self.assertEqual(len(re.findall(r"\b" + name + r"\s*=", blocks)), 1, name)
            self.assertEqual(len(re.findall(r"\b" + name + r"\s*=", items)), 1, name)
            self.assertIn("registerSimpleBlockItem(ModBlocks." + name + ")", items)
        for name in ("ROTARY_CONVERSION_ENTITY", "STEAM_BOILER_ENTITY", "THERMOELECTRIC_GENERATOR_ENTITY",
                     "MECHANICAL_CONVEYOR_ENTITY", "COMPACT_GEARBOX_ENTITY"):
            self.assertEqual(len(re.findall(r"\b" + name + r"\s*=", blocks)), 1, name)
        self.assertIn("Set.of(ELECTROMAGNETIC_GENERATOR.get(), ELECTRIC_MOTOR.get(), STEAM_ENGINE.get())", blocks)
        self.assertIn("Set.of(COPPER_BUSBAR.get(), INSULATED_CONDUIT.get(), INDUSTRIAL_CIRCUIT_SWITCH.get())", blocks)
        self.assertLess(blocks.index("INDUSTRIAL_CIRCUIT_SWITCH ="), blocks.index("COPPER_CABLE_ENTITY ="))

if __name__ == "__main__":
    unittest.main()
