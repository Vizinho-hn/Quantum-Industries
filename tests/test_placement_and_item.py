#!/usr/bin/env python3
"""Contract checks for placement variants, item preview and translations.

These are static resource checks; in-game facing and GUI appearance also require client QA.
"""
import json
import struct
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"
ANGLES = {"north": 0, "east": 90, "south": 180, "west": 270}


class PlacementAndItemTest(unittest.TestCase):
    def load(self, relative):
        return json.loads((ASSETS / relative).read_text(encoding="utf-8"))

    def test_research_analyzers_have_all_directional_models(self):
        for name in ("scientific_analyzer", "scientific_analyzer_tier_1", "voltaic_pile", "electromechanical_press"):
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
        """GUI renders a flat 2D icon; the 3D bench is a two-block model that cannot fit the slot."""
        definition = self.load("items/empirical_assay_bench.json")["model"]
        self.assertEqual(definition["type"], "minecraft:select")
        self.assertEqual(definition["property"], "minecraft:display_context")
        self.assertEqual(definition["cases"][0]["when"], "gui")
        self.assertEqual(definition["cases"][0]["model"]["model"], "quantumindustries:item/empirical_assay_bench_gui")
        self.assertEqual(definition["fallback"]["model"], "quantumindustries:item/empirical_assay_bench")

        gui = self.load("models/item/empirical_assay_bench_gui.json")
        self.assertEqual(gui["parent"], "minecraft:item/generated")
        self.assertEqual(gui["textures"]["layer0"], "quantumindustries:item/empirical_assay_bench")

        model = self.load("models/item/empirical_assay_bench.json")
        self.assertEqual(model["parent"], "quantumindustries:block/empirical_assay_bench")

    def test_gui_icon_texture_exists_at_item_scale(self):
        icon = ASSETS / "textures/item/empirical_assay_bench.png"
        self.assertTrue(icon.is_file())
        raw = icon.read_bytes()
        self.assertEqual(raw[:8], b"\x89PNG\r\n\x1a\n")
        self.assertEqual(raw[12:16], b"IHDR")
        width, height = struct.unpack(">II", raw[16:24])
        self.assertEqual((width, height), (32, 32))
        colour_type = raw[25]  # 6 = RGBA, so the icon can have a transparent background
        self.assertEqual(colour_type, 6)

    def test_no_item_renders_a_multi_block_model_in_the_gui(self):
        """A block model wider than one block reaching an item slot is the bug this guards.

        Covers both shapes an item JSON can take: a plain model (rendered in every context,
        including the GUI) and a select on display_context whose `gui` case is what the slot
        actually draws. Also follows one item-model -> block-model parent hop.
        """
        scanned = 0
        for definition in sorted((ASSETS / "items").glob("*.json")):
            for target in self.gui_model_references(self.load(f"items/{definition.name}")["model"]):
                for block in self.reachable_block_models(target):
                    xs = [v for element in block.get("elements", []) for v in (element["from"][0], element["to"][0])]
                    if not xs:
                        continue
                    scanned += 1
                    self.assertLessEqual(
                        max(xs) - min(xs), 16.0 + 1e-6,
                        f"{definition.name} draws the {max(xs) - min(xs)}px-wide {block['__id']} in the GUI slot",
                    )
        self.assertGreater(scanned, 0, "the GUI-slot block-model scan matched nothing")

    @staticmethod
    def gui_model_references(model):
        """The model ids this item definition renders in the GUI slot."""
        if model.get("type") == "minecraft:model":
            return [model["model"]]
        references = []
        for case in model.get("cases", []):
            if case.get("when") == "gui":
                references.extend(PlacementAndItemTest.gui_model_references(case["model"]))
        if references:
            return references
        return PlacementAndItemTest.gui_model_references(model.get("fallback", {}))

    def reachable_block_models(self, target):
        """Block models an item-model reference renders, following one item-model parent hop."""
        path = f"models/{target.split(':')[1]}.json"
        if not (ASSETS / path).is_file():
            return
        node = self.load(path)
        if target.startswith("quantumindustries:block/"):
            node["__id"] = target
            yield node
            return
        parent = node.get("parent", "")
        if parent.startswith("quantumindustries:block/"):
            parent_path = f"models/{parent.split(':')[1]}.json"
            if (ASSETS / parent_path).is_file():
                block = self.load(parent_path)
                block["__id"] = parent
                yield block

    def test_item_models_reference_existing_textures(self):
        """An item model pointing at a texture that was never created draws the missing-texture
        checkerboard in the inventory slot. Scoped to models reachable from an item definition
        so dead files do not fail the suite."""
        checked = 0
        for definition in sorted((ASSETS / "items").glob("*.json")):
            model = self.load(f"items/{definition.name}")["model"]
            for model_id in self.all_model_references(model):
                if not model_id.startswith("quantumindustries:item/"):
                    continue
                node = self.load(f"models/{model_id.split(':', 1)[1]}.json")
                for key, texture in node.get("textures", {}).items():
                    if not texture.startswith("quantumindustries:"):
                        continue
                    checked += 1
                    suffix = texture.split(":", 1)[1]
                    self.assertTrue(
                        (ASSETS / "textures" / f"{suffix}.png").is_file(),
                        f"{definition.name} renders {model_id}, whose {key} -> {texture} has no "
                        f"texture file",
                    )
        self.assertGreater(checked, 0, "the item icon texture scan checked nothing")

    @staticmethod
    def all_model_references(model):
        """Every model id an item definition can render, in any case and fallback."""
        if model.get("type") == "minecraft:model":
            return [model["model"]]
        references = []
        for case in model.get("cases", []):
            references.extend(PlacementAndItemTest.all_model_references(case["model"]))
        for key in ("fallback", "on_true", "on_false"):
            if key in model:
                references.extend(PlacementAndItemTest.all_model_references(model[key]))
        return references

    def test_display_name_in_both_locales(self):
        for locale, title in (("pt_br", "Analisador de Materiais"), ("en_us", "Material Analyzer")):
            words = self.load(f"lang/{locale}.json")
            self.assertEqual(words["item.quantumindustries.empirical_assay_bench"], title)
            self.assertEqual(words["block.quantumindustries.empirical_assay_bench"], title)
            self.assertEqual(words["gui.quantumindustries.assay.title"], title)


if __name__ == "__main__":
    unittest.main()
