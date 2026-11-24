"""Check the declared research graph, scientific evidence integration, and the frozen migration order."""
import re
import unittest
from pathlib import Path

RESEARCH = Path(__file__).resolve().parents[1] / "src/main/java/br/com/quantumindustries/research"


class ResearchCatalogTest(unittest.TestCase):
    def test_catalog_graph(self):
        source = (RESEARCH / "ResearchCatalog.java").read_text(encoding="utf-8")
        entries = re.findall(r'new Study\("([^"]+)"[^\n]*?, [12], List.of\(([^)]*)\)', source)
        self.assertEqual(len(entries), 22)
        graph = {key: re.findall(r'"([^"]+)"', deps) for key, deps in entries}
        self.assertEqual(len(graph), len(entries))
        self.assertEqual([key for key, deps in graph.items() if not deps], ["material_observation"])
        self.assertEqual(graph["conductivity"], ["material_observation"])
        self.assertEqual(graph["copper_wire"], ["conductivity"])
        self.assertEqual(graph["electromagnetic_induction"], ["manual_dynamo_design"])
        self.assertEqual(graph["galvanic_storage"], ["electromagnetic_induction"])
        self.assertEqual(graph["electromechanical_forming"], ["electromagnetic_induction"])
        self.assertEqual(graph["hydraulic_power"], ["mechanical_transmission"])
        self.assertEqual(graph["industrial_generation"], ["alternating_current", "electromechanical_forming"])
        self.assertEqual(graph["electromagnetic_excitation"], ["electromagnetic_induction", "galvanic_storage"])
        self.assertEqual(graph["alternating_current"], ["electromagnetic_excitation"])
        self.assertEqual(graph["mechanical_energy_storage"], ["mechanical_transmission", "metal_sheets"])
        for method in ("Method.INSPECTION", "Method.MECHANICAL", "Method.HEATING", "Method.REACTION"):
            self.assertIn(method, source)
        self.assertIn("ResearchEvidence.AnyOf", source)
        visited = set()

        def visit(key, path):
            self.assertIn(key, graph)
            self.assertNotIn(key, path)
            if key in visited:
                return
            for dep in graph[key]:
                visit(dep, path | {key})
            visited.add(key)

        for key in graph:
            visit(key, set())


if __name__ == "__main__":
    unittest.main()
