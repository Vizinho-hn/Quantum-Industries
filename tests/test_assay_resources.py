#!/usr/bin/env python3
"""Asset integration checks; lifecycle behavior is verified on a dedicated server."""
import json
from pathlib import Path
import struct
import unittest

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/quantumindustries'
PREFIX='gui.quantumindustries.assay.'
MATERIALS='copper iron gold coal diamond quartz redstone lapis emerald amethyst'.split()
METHODS='inspection heating reaction mechanical'.split()

class AssayResourcesTest(unittest.TestCase):
    def test_all_observations_and_legacy_history_have_translations(self):
        for locale in ('pt_br','en_us'):
            strings=json.loads((ASSETS/f'lang/{locale}.json').read_text())
            for material in MATERIALS:
                for method in METHODS+['galvanic']:
                    self.assertTrue(strings[PREFIX+f'observation.{material}.{method}'].strip())
            for method in METHODS:
                self.assertIn(PREFIX+'method.'+method,strings)

    def test_era_zero_recipes_do_not_require_brass(self):
        for tool in ('magnifying_glass','manual_bellows','assay_flask'):
            recipe=json.loads((ROOT/f'src/main/resources/data/quantumindustries/recipe/{tool}.json').read_text())
            self.assertNotIn('brass',json.dumps(recipe))
            definition=json.loads((ASSETS/f'items/{tool}.json').read_text())
            self.assertTrue((ASSETS/('models/'+definition['model']['model'].split(':')[1]+'.json')).is_file())

    def test_shared_geometry_and_static_halves_do_not_duplicate_moving_parts(self):
        mesh=json.loads((ASSETS/'assay/workshop.json').read_text())['boxes']
        groups={b['group'] for b in mesh}
        self.assertTrue({'base','screw','head','lupa','bellows_lid','bellows_leather','dropper','tray','liquid'}<=groups)
        moving={b['name'] for b in mesh if b['group']!='base'}
        for part in ('head','foot'):
            elements=json.loads((ASSETS/f'models/block/empirical_assay_bench_{part}.json').read_text())['elements']
            self.assertTrue(all(e['name'] not in moving for e in elements))
        source=json.loads((ROOT/'design/assay-v2/empirical_workshop.bbmodel').read_text())
        self.assertEqual(len(source['elements']),len(mesh))
        self.assertEqual(len(source['animations']),4)

    def test_vfx_strip_matches_export_manifest(self):
        meta=json.loads((ROOT/'design/assay-v2/vfx-export.json').read_text())
        png=(ASSETS/'textures/assay/burner.png').read_bytes()
        self.assertGreaterEqual(len(png),24, 'PNG must contain its signature and IHDR header')
        self.assertEqual(png[:8],b'\x89PNG\r\n\x1a\n', 'invalid PNG signature')
        self.assertEqual(png[12:16],b'IHDR', 'PNG must begin with an IHDR chunk')
        width,height=struct.unpack('>II',png[16:24])
        self.assertEqual((width,height),(meta['width'],meta['height']*meta['frames']))
        self.assertEqual(meta['frames'],20)

if __name__=='__main__':
    unittest.main()
