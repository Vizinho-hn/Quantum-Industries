#!/usr/bin/env python3
"""Catalog translation and resource integrity checks; behavior uses runtime harness."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/quantumindustries'
MATERIALS = 'copper iron gold coal diamond quartz redstone lapis emerald amethyst'.split()
METHODS = 'mechanical heating galvanic'.split()
PREFIX = 'gui.quantumindustries.assay.'

class AssayResourcesTest(unittest.TestCase):
    def test_thirty_observations_in_both_languages(self):
        expected = {PREFIX + f'observation.{m}.{t}' for m in MATERIALS for t in METHODS}
        for locale in ('pt_br', 'en_us'):
            strings = json.loads((ASSETS / f'lang/{locale}.json').read_text())
            self.assertEqual({k for k in strings if k.startswith(PREFIX+'observation.')}, expected)
            for key in expected:
                self.assertTrue(strings[key].strip(), key)
            for material in MATERIALS:
                self.assertIn(PREFIX+'material.'+material, strings)
            for method in METHODS:
                self.assertIn(PREFIX+'method.'+method, strings)

    def test_copper_and_redstone_scientific_scope(self):
        pt = json.loads((ASSETS/'lang/pt_br.json').read_text())
        self.assertIn('maleável', pt[PREFIX+'observation.copper.mechanical'].lower())
        self.assertIn('escurece', pt[PREFIX+'observation.copper.heating'].lower())
        self.assertIn('ficcional', pt[PREFIX+'observation.redstone.galvanic'].lower())
        self.assertIn('conclusiva', pt[PREFIX+'observation.coal.galvanic'].lower())
        observations=' '.join(v for k,v in pt.items() if k.startswith(PREFIX+'observation.')).lower()
        for forbidden in ('dúctil','chama verde','resistividade','ohm','cfc'):
            self.assertNotIn(forbidden,observations)

    def test_tool_models_and_recipes_resolve(self):
        for tool in ('magnifying_glass','brass_weights'):
            definition = json.loads((ASSETS/f'items/{tool}.json').read_text())
            name=definition['model']['model'].split(':')[1]
            self.assertTrue((ASSETS/f'models/{name}.json').is_file())
            recipe=ROOT/f'src/main/resources/data/quantumindustries/recipe/{tool}.json'
            self.assertTrue(json.loads(recipe.read_text()))
        weights=json.loads((ROOT/'src/main/resources/data/quantumindustries/recipe/brass_weights.json').read_text())
        self.assertEqual(weights['key']['I'],'minecraft:iron_ingot')
        self.assertFalse((ROOT/'src/main/resources/data/quantumindustries/recipe/flame_test_kit.json').exists())
        self.assertTrue((ASSETS/'textures/gui/empirical_instruments.png').is_file())

if __name__ == '__main__':
    unittest.main()
