import base64
import json
import struct
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/quantumindustries'
DATA = ROOT / 'src/main/resources/data/quantumindustries'
NAMES = ('mechanical_sawmill', 'mechanical_shaft_block', 'primitive_water_wheel')


class MechanicalResourcesTest(unittest.TestCase):
    def test_every_vanilla_log_family_has_a_species_preserving_process(self):
        files = list((DATA / 'recipe/sawing').glob('*.json'))
        self.assertEqual(len(files), 11)
        seen = set()
        for file in files:
            recipe = json.loads(file.read_text(encoding='utf8'))
            self.assertEqual(recipe['type'], 'quantumindustries:mechanical_processing')
            self.assertEqual(recipe['result'], {'id': 'minecraft:' + file.stem + '_planks', 'count': 6})
            self.assertEqual(recipe['mechanical_load'], 6)
            self.assertEqual(recipe['ticks'], 100)
            self.assertEqual(recipe['research'], 'mechanical_transmission')
            self.assertEqual(len(recipe['inputs']), 4)
            self.assertTrue(seen.isdisjoint(recipe['inputs']))
            seen.update(recipe['inputs'])
        self.assertEqual(len(seen), 44)

    def test_static_and_animated_geometry_are_not_drawn_twice(self):
        parts = json.loads((ASSETS / 'mechanical/parts.json').read_text(encoding='utf8'))
        for name in NAMES:
            boxes = parts[name]
            base = json.loads((ASSETS / f'models/block/{name}.json').read_text(encoding='utf8'))
            item = json.loads((ASSETS / f'models/item/{name}.json').read_text(encoding='utf8'))
            self.assertEqual(len(base['elements']), sum(b['group'] == 'base' for b in boxes))
            self.assertEqual(len(item['elements']), sum(b['group'] != 'crank' for b in boxes))
            for box in boxes:
                self.assertTrue(all(0 <= a < b <= 16 for a, b in zip(box['from'], box['to'])))
            bb = json.loads((ROOT / f'design/mechanical/{name}.bbmodel').read_text(encoding='utf8'))
            self.assertEqual(len(bb['elements']), len(boxes))
            embedded = base64.b64decode(bb['textures'][0]['source'].split(',', 1)[1])
            self.assertEqual(embedded, (ASSETS/'textures/block/mechanical_workshop.png').read_bytes())
            self.assertEqual(struct.unpack('>II', embedded[16:24]), (64, 32))

    def test_orientations_loot_and_item_definitions(self):
        for name in NAMES:
            states = json.loads((ASSETS / f'blockstates/{name}.json').read_text(encoding='utf8'))['variants']
            self.assertEqual(set(states), {'axis=x', 'axis=y', 'axis=z'} if 'shaft' in name else
                             {'facing=north','facing=east','facing=south','facing=west'})
            definition = json.loads((ASSETS / f'items/{name}.json').read_text(encoding='utf8'))
            self.assertEqual(definition['model']['model'], 'quantumindustries:item/'+name)
            loot = json.loads((DATA / f'loot_table/blocks/{name}.json').read_text(encoding='utf8'))
            self.assertEqual(loot['pools'][0]['entries'][0]['name'], 'quantumindustries:'+name)

    def test_new_machine_translations_exist_on_both_sides(self):
        for locale in ('pt_br', 'en_us'):
            lang = json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf8'))
            for name in NAMES:
                self.assertTrue(lang['block.quantumindustries.'+name].strip())
            for key in ('sawmill.help','sawmill.status','mechanical.help','mechanical.running',
                        'mechanical.stopped','mechanical.network','wheel.status'):
                self.assertTrue(lang['gui.quantumindustries.'+key].strip())


if __name__ == '__main__':
    unittest.main()
