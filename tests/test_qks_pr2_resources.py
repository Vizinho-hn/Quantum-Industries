"""PR 2 resource contracts, independent of the concurrent Java implementation."""
import itertools
import json
import math
import struct
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/quantumindustries'
DATA = ROOT / 'src/main/resources/data/quantumindustries'
AXIS = ('small_cogwheel', 'large_cogwheel', 'iron_flywheel', 'mechanical_pulley')
WHEELS = ('medium_water_wheel', 'large_water_wheel')
BLOCKS = AXIS + WHEELS
IDS = BLOCKS + ('drive_belt',)
FACES = {'north', 'south', 'east', 'west', 'up', 'down'}


def load(path):
    return json.loads(path.read_text(encoding='utf8'))


def geometry(name):
    return load(ASSETS / f'geckolib/models/block/{name}.geo.json')['minecraft:geometry'][0]


def validate_uv(cube, width, height):
    if set(cube['uv']) != FACES:
        raise ValueError('Six per-face UVs required by QKS')
    for face in cube['uv'].values():
        for start, span, limit in zip(face['uv'], face['uv_size'], (width, height)):
            if span <= 0 or not 0 <= start < start + span <= limit:
                raise ValueError('UV outside PNG or degenerate')


def corners(cube):
    angle = math.radians(cube.get('rotation', [0, 0, 0])[2])
    pivot = cube.get('pivot', [0, 8, 0])
    for offset in itertools.product((0, 1), repeat=3):
        x, y, z = [o + s * t for o, s, t in zip(cube['origin'], cube['size'], offset)]
        dx, dy = x-pivot[0], y-pivot[1]
        yield (pivot[0]+dx*math.cos(angle)-dy*math.sin(angle),
               pivot[1]+dx*math.sin(angle)+dy*math.cos(angle), z)


class QksPr2Resources(unittest.TestCase):
    def test_all_six_rotors_use_centered_z_axles_and_in_bounds_png_uvs(self):
        for name in BLOCKS:
            with self.subTest(name=name):
                document = load(ASSETS / f'geckolib/models/block/{name}.geo.json')
                self.assertEqual(document['format_version'], '1.12.0')
                geo = geometry(name)
                texture = 'water_wheel_d' if name in WHEELS else name
                png = (ASSETS / f'textures/block/{texture}.png').read_bytes()
                dimensions = struct.unpack('>II', png[16:24])
                self.assertEqual(dimensions, (geo['description']['texture_width'], geo['description']['texture_height']))
                bones = {b['name']: b for b in geo['bones']}
                self.assertEqual(set(bones), {'rotor'} if name in AXIS[:2] or name in WHEELS else {'base', 'rotor'})
                self.assertEqual(bones['rotor']['pivot'], [0, 8, 0])
                cubes = bones['rotor']['cubes']
                if name in WHEELS:
                    width = 4 if name == 'medium_water_wheel' else 6
                    sockets = [c for c in cubes if c.get('name') == 'Encaixe rente ao cubo']
                    self.assertEqual(len(sockets), 2)
                    self.assertTrue(all(c['origin'][:2] == [-width/2, 8-width/2]
                                        and c['size'][:2] == [width, width] for c in sockets))
                    self.assertAlmostEqual(sockets[0]['origin'][2], -sockets[1]['origin'][2]-sockets[1]['size'][2])
                else:
                    self.assertTrue(any(c['origin'] == [-2, 6, -8] and c['size'] == [4, 4, 16] for c in cubes))
                for bone in bones.values():
                    for cube in bone['cubes']:
                        self.assertTrue(all(s >= (.04 if name in WHEELS else .5) for s in cube['size']))
                        validate_uv(cube, *dimensions)

    def test_uv_checker_rejects_out_of_bounds_and_zero_area_faces(self):
        for start, span in ((127, 2), (0, 0), (-1, 1)):
            cube = {'uv': {face: {'uv': [start, 0], 'uv_size': [span, 1]} for face in FACES}}
            with self.assertRaises(ValueError):
                validate_uv(cube, 128, 128)

    def test_teeth_paddles_and_true_multiblock_dimensions(self):
        for name, count in (('small_cogwheel', 8), ('large_cogwheel', 16)):
            cubes = geometry(name)['bones'][0]['cubes']
            self.assertEqual(sum(c.get('name', '').startswith('Tooth ') for c in cubes), count)
        for name, radius, paddles in (('medium_water_wheel', 21.5, 12), ('large_water_wheel', 37.5, 12)):
            rotor = next(b for b in geometry(name)['bones'] if b['name'] == 'rotor')
            self.assertEqual(sum(c.get('name') == 'Fundo da caçamba' for c in rotor['cubes']), paddles)
            points = [p for c in rotor['cubes'] for p in corners(c)]
            self.assertAlmostEqual(max(math.hypot(x, y-8) for x, y, z in points), radius, places=3)
            self.assertTrue(all(-8 < z < 8 for x, y, z in points))

    def test_resource_chains_recipes_and_self_drops(self):
        for name in IDS:
            definition = load(ASSETS / f'items/{name}.json')
            self.assertEqual(definition['model'], {'type': 'minecraft:model', 'model': f'quantumindustries:item/{name}'})
            item = load(ASSETS / f'models/item/{name}.json')
            self.assertEqual(item['textures']['layer0'], f'quantumindustries:item/{name}')
            self.assertTrue((ASSETS / f'textures/item/{name}.png').is_file())
            recipe = load(DATA / f'recipe/{name}.json')
            self.assertEqual(recipe, {'type': 'quantumindustries:foundations', 'craft': name})
            if name == 'drive_belt':
                continue
            states = load(ASSETS / f'blockstates/{name}.json')['variants']
            self.assertEqual(set(states), {'axis='+a for a in 'xyz'} if name in AXIS else {'facing='+a for a in ('north','east','south','west')})
            block = load(ASSETS / f'models/block/{name}.json')
            self.assertEqual(block['elements'], [])  # Native renderer owns base AND rotor.
            texture = 'water_wheel_d' if name in WHEELS else name
            self.assertEqual(block['textures']['particle'], f'quantumindustries:block/{texture}')
            loot = load(DATA / f'loot_table/blocks/{name}.json')['pools'][0]
            self.assertEqual(loot['entries'][0]['name'], 'quantumindustries:'+name)
            self.assertIn({'condition': 'minecraft:survives_explosion'}, loot['conditions'])

    def test_localized_names_tooltips_and_belt_feedback(self):
        for locale in ('en_us','pt_br'):
            lang = load(ASSETS / f'lang/{locale}.json')
            for name in IDS:
                kind = 'item' if name == 'drive_belt' else 'block'
                self.assertTrue(lang[f'{kind}.quantumindustries.{name}'])
                self.assertTrue(lang[f'tooltip.quantumindustries.{name}'])
            for suffix in ('select_first','linked_open','linked_crossed','unlinked','invalid_target','too_far'):
                self.assertTrue(lang['message.quantumindustries.belt.'+suffix])
