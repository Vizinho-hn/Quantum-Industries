import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
ASSETS = RESOURCES / "assets/quantumindustries"
VARIANTS = (
    ("pneumatic_tube", "Transportador de Itens de Vidro e Cobre", "Copper-Glass Item Transporter"),
    ("fluid_pipe", "Transportador de Líquidos de Cobre", "Copper Liquid Transporter"),
    ("glass_pipe", "Transportador de Líquidos de Vidro", "Glass Liquid Transporter"),
    ("gas_pipe", "Transportador de Gases de Cobre", "Copper Gas Transporter"),
    ("copper_busbar", "Transportador de Energia de Cobre Nu", "Bare Copper Energy Transporter"),
    ("insulated_conduit", "Transportador de Energia de Cobre Isolado", "Insulated Copper Energy Transporter"),
)


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


class IndustrialTransportTest(unittest.TestCase):
    def test_four_transport_categories_keep_their_registered_identifiers_and_names(self):
        translations = [read_json(ASSETS / f"lang/{lang}.json") for lang in ("pt_br", "en_us")]
        for name, *labels in VARIANTS:
            with self.subTest(variant=name):
                self.assertTrue((ASSETS / f"blockstates/{name}.json").is_file())
                self.assertTrue((ASSETS / f"items/{name}.json").is_file())
                for language, label in zip(translations, labels):
                    self.assertEqual(language[f"block.quantumindustries.{name}"], label)
                    self.assertEqual(language[f"item.quantumindustries.{name}"], label)

    def test_gas_models_have_all_six_connections(self):
        states = read_json(ASSETS / "blockstates/gas_pipe.json")["multipart"]
        sides = {side for part in states[1:] for side, enabled in part["when"].items() if enabled == "true"}
        self.assertEqual(sides, {"east", "west", "north", "south", "up", "down"})
        for part in states:
            model = part["apply"]["model"].removeprefix("quantumindustries:")
            self.assertTrue((ASSETS / f"models/{model}.json").is_file())
        self.assertTrue((ROOT / "design/mechanical/gas_pipe.bbmodel").is_file())

    def test_codex_chapters_are_translated(self):
        chapters = ("industrial_transport_systems", "item_transport", "liquid_transport", "gas_transport",
                    "energy_transport", "steam_boiler", "steam_engine", "steam_distribution")
        for lang in ("pt_br", "en_us"):
            strings = read_json(ASSETS / f"lang/{lang}.json")
            for chapter in chapters:
                with self.subTest(lang=lang, chapter=chapter):
                    for key in ("title", "subtitle", "paragraph_0", "paragraph_1", "operation"):
                        self.assertTrue(strings[f"guidebook.quantumindustries.{chapter}.{key}"])


if __name__ == "__main__":
    unittest.main()
