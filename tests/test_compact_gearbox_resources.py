import base64
import json
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


class CompactGearboxResources(unittest.TestCase):
    def test_baked_frame_and_moving_meshes_are_separate(self):
        states = read_json(ASSETS / "blockstates/compact_gearbox.json")["variants"]
        for excluded in "xyz":
            with self.subTest(excluded=excluded):
                model = states["axis=" + excluded]["model"].split(":")[1]
                frame = read_json(ASSETS / "models" / (model + ".json"))
                self.assertFalse(any(e["name"].startswith(("Steel", "Bevel", "Bronze gear"))
                                     for e in frame["elements"]))
                path = ASSETS / f"geckolib/models/block/compact_gearbox_{excluded}.geo.json"
                bones = read_json(path)["minecraft:geometry"][0]["bones"]
                self.assertEqual({b["name"][-1] for b in bones}, set("xyz") - {excluded})
                for bone in bones:
                    self.assertEqual(bone["pivot"], [0, 8, 0])
                    self.assertGreater(len(bone["cubes"]), 16)
                    axis = "xyz".index(bone["name"][-1])
                    # Shaft ends reach the active faces, at the center of those faces.
                    offset = [8, 0, 8][axis]
                    origins = [c["origin"][axis] + offset for c in bone["cubes"]]
                    ends = [o + c["size"][axis] for o, c in zip(origins, bone["cubes"])]
                    self.assertEqual((min(origins), max(ends)), (0, 16))
                    self.assertTrue(all(s > 0 for c in bone["cubes"] for s in c["size"]))

    def test_blockbench_source_contains_the_exported_geometry_and_atlas(self):
        edit = read_json(ROOT / "design/mechanical/compact_gearbox.bbmodel")
        item = read_json(ASSETS / "models/item/compact_gearbox.json")
        self.assertEqual([(e["from"], e["to"]) for e in edit["elements"]],
                         [(e["from"], e["to"]) for e in item["elements"]])
        self.assertEqual(len(edit["outliner"]), 3)
        self.assertEqual({i for group in edit["outliner"] for i in group["children"]},
                         {e["uuid"] for e in edit["elements"]})
        self.assertEqual(base64.b64decode(edit["textures"][0]["source"].split(",")[1]),
                         (ASSETS / "textures/block/compact_gearbox.png").read_bytes())
