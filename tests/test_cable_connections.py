"""Check the rendered cable reaches exactly its connected block faces."""
import itertools
import json
from pathlib import Path
import unittest

ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/quantumindustries"
DIRECTIONS = ("north", "east", "south", "west", "up", "down")


def model_elements(model):
    data = json.loads((ASSETS / "models" / (model.split(":")[1] + ".json")).read_text())
    return data.get("elements") or model_elements(data["parent"])


def rotate(point, x, y):
    a, b, c = (v - 8 for v in point)
    for _ in range(x // 90):
        b, c = c, -b
    for _ in range(y // 90):
        a, c = -c, a
    return a + 8, b + 8, c + 8


class CableConnections(unittest.TestCase):
    def test_all_connection_masks_reach_only_connected_faces(self):
        for cable in ("copper_busbar", "insulated_conduit"):
            data = json.loads((ASSETS / "blockstates" / f"{cable}.json").read_text())
            for mask in itertools.product((False, True), repeat=6):
                state = dict(zip(DIRECTIONS, (str(v).lower() for v in mask)))
                if "multipart" in data:
                    parts = [p["apply"] for p in data["multipart"]
                             if all(state[k] == v for k, v in p.get("when", {}).items())]
                else:
                    parts = [data["variants"]["facing=east,shape=straight"]]
                points = [rotate(p, part.get("x", 0), part.get("y", 0))
                          for part in parts for e in model_elements(part["model"])
                          for p in itertools.product(*zip(e["from"], e["to"]))]
                bounds = [(min(p[i] for p in points), max(p[i] for p in points)) for i in range(3)]
                reaches = (bounds[2][0] == 0, bounds[0][1] == 16,
                           bounds[2][1] == 16, bounds[0][0] == 0,
                           bounds[1][1] == 16, bounds[1][0] == 0)
                with self.subTest(cable=cable, connections=mask):
                    self.assertEqual(reaches, mask)


if __name__ == "__main__":
    unittest.main()
