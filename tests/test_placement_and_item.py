#!/usr/bin/env python3
"""Contract checks for placement variants, item preview and translations.

These are static resource checks; in-game facing and GUI appearance also require client QA.
"""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"
ANGLES = {"north": 0, "east": 90, "south": 180, "west": 270}


class PlacementAndItemTest(unittest.TestCase):
    def load(self, relative):
        return json.loads((ASSETS / relative).read_text(encoding="utf-8"))

    def test_research_analyzers_have_all_directional_models(self):
        for name in ("scientific_analyzer", "scientific_analyzer_tier_1", "galvanic_accumulator", "electromechanical_press"):
            variants = self.load(f"blockstates/{name}.json")["variants"]
            self.assertEqual(set(variants), {f"facing={direction}" for direction in ANGLES})
            for direction, angle in ANGLES.items():
                self.assertEqual(variants[f"facing={direction}"].get("y", 0), angle)

    def test_empirical_analyzer_rotates_both_block_halves(self):
        variants = self.load("blockstates/empirical_assay_bench.json")["variants"]
        for direction, angle in ANGLES.items():
            for part in ("foot", "head"):
                self.assertEqual(variants[f"facing={direction},part={part}"].get("y", 0), angle)
        for name in ("ResearchTableBlock", "EmpiricalAssayBenchBlock"):
            source = (ROOT / f"src/main/java/br/com/quantumindustries/block/{name}.java").read_text(encoding="utf-8")
            self.assertIn("getHorizontalDirection().getOpposite()", source)

    def test_item_uses_compact_centered_gui_model(self):
        definition = self.load("items/empirical_assay_bench.json")
        self.assertEqual(definition["model"]["model"], "quantumindustries:item/empirical_assay_bench")
        model = self.load("models/item/empirical_assay_bench.json")
        self.assertEqual(model["parent"], "quantumindustries:block/empirical_assay_bench")
        self.assertLessEqual(max(model["display"]["gui"]["scale"]), .32)
        self.assertIn("rotation", model["display"]["gui"])

    def test_display_name_in_both_locales(self):
        for locale, title in (("pt_br", "Analisador de Materiais"), ("en_us", "Material Analyzer")):
            words = self.load(f"lang/{locale}.json")
            self.assertEqual(words["item.quantumindustries.empirical_assay_bench"], title)
            self.assertEqual(words["block.quantumindustries.empirical_assay_bench"], title)
            self.assertEqual(words["gui.quantumindustries.assay.title"], title)


if __name__ == "__main__":
    unittest.main()
