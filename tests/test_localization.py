"""Resource contracts for both supported languages; runtime rendering is tested in NeoForge."""
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"


class LocalizationContracts(unittest.TestCase):
    def test_languages_cover_same_keys_and_format_arguments(self):
        pt = json.loads((ASSETS / "lang/pt_br.json").read_text())
        en = json.loads((ASSETS / "lang/en_us.json").read_text())
        self.assertEqual(pt.keys(), en.keys())
        for key in pt:
            with self.subTest(key=key):
                self.assertTrue(pt[key].strip())
                self.assertTrue(en[key].strip())
                pattern = r"(?<!%)%(?!%)(?:\d+\$)?[sdif]"
                self.assertEqual(re.findall(pattern, pt[key]), re.findall(pattern, en[key]))

    def test_guidebook_and_study_content_exists_in_both_languages(self):
        java = ROOT / "src/main/java/br/com/quantumindustries/research"
        for locale in ("pt_br", "en_us"):
            translations = json.loads((ASSETS / f"lang/{locale}.json").read_text())
            for file in ("GuidebookCatalog.java", "ResearchCatalog.java"):
                keys = re.findall(r'"((?:guidebook|research)\.quantumindustries\.[^"]+)"', (java / file).read_text())
                self.assertTrue(keys)
                for key in keys:
                    with self.subTest(locale=locale, key=key):
                        self.assertIn(key, translations)

    def test_visible_item_assets_have_localized_names(self):
        for locale in ("pt_br", "en_us"):
            translations = json.loads((ASSETS / f"lang/{locale}.json").read_text())
            for asset in (ASSETS / "items").glob("*.json"):
                if asset.stem == "dynamo_crank_installed":  # Internal render-only model.
                    continue
                with self.subTest(locale=locale, item=asset.stem):
                    self.assertTrue(any(f"{kind}.quantumindustries.{asset.stem}" in translations
                                        for kind in ("item", "block")))
