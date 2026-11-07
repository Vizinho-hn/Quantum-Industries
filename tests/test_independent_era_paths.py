"""Contracts for independent scientific branches across the game's eras.

Static tests complement (but do not replace) NeoForge gameplay/Gradle verification.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "src/main/java/br/com/quantumindustries/research/ResearchCatalog.java"
SCREEN = ROOT / "src/main/java/br/com/quantumindustries/client/ResearchTableScreen.java"
SERVER = ROOT / "src/main/java/br/com/quantumindustries/research/WorldResearchData.java"


def research_dependencies():
    source = CATALOG.read_text(encoding="utf-8")
    entries = list(re.finditer(r'new Study\("([^"]+)"', source))
    graph = {}
    for i, entry in enumerate(entries):
        fragment = source[entry.start():entries[i + 1].start() if i + 1 < len(entries) else source.index("public record Branch")]
        match = re.search(r',\s*[1-5]\s*,\s*List\.of\(([^)]*)\)', fragment)
        if not match:
            raise AssertionError(f"Cannot read prerequisites for {entry.group(1)}")
        graph[entry.group(1)] = re.findall(r'"([^"]+)"', match.group(1))
    return graph


class IndependentEraPathsTest(unittest.TestCase):
    def test_all_research_still_has_valid_acyclic_dependencies(self):
        graph = research_dependencies()
        self.assertGreaterEqual(len(graph), 20)
        self.assertEqual([k for k, v in graph.items() if not v], ["material_observation"])

        def walk(key, seen):
            self.assertIn(key, graph)
            self.assertNotIn(key, seen, f"Research cycle involving {key}")
            for dependency in graph[key]:
                walk(dependency, seen | {key})

        for name in graph:
            walk(name, set())

    def test_resource_and_workshop_tracks_do_not_require_electric_era(self):
        graph = research_dependencies()

        def ancestors(key):
            dependencies = set(graph[key])
            for dependency in graph[key]:
                dependencies.update(ancestors(dependency))
            return dependencies

        optional_tracks = (
            "hydraulic_power",
            "fluid_hydrostatics",
            "pneumatic_transport",
            "thermal_annealing",
            "wire_drawing",
        )
        for key in optional_tracks:
            with self.subTest(study=key):
                parents = ancestors(key)
                self.assertNotIn("manual_dynamo_design", parents)
                self.assertNotIn("electromagnetic_induction", parents)
                self.assertIn("material_observation", parents)

    def test_energy_technology_still_requires_the_relevant_knowledge(self):
        graph = research_dependencies()
        self.assertIn("manual_dynamo_design", graph["electromagnetic_induction"])
        self.assertIn("electromagnetic_induction", graph["galvanic_storage"])
        self.assertIn("galvanic_storage", graph["load_protection"])

    def test_era_tabs_are_navigation_not_gameplay_gates(self):
        screen = SCREEN.read_text(encoding="utf-8")
        self.assertIn("ResearchCatalog.hasStudiesInEra(next)", screen)
        self.assertIn("ResearchCatalog.hasStudiesInEra(i + 1)", screen)
        self.assertNotIn('next == 2 && complete(ResearchCatalog.indexOf("manual_dynamo_design"))', screen)
        catalog = CATALOG.read_text(encoding="utf-8")
        self.assertIn("public static boolean hasStudiesInEra(int era)", catalog)

    def test_server_still_validates_study_evidence_and_prerequisites(self):
        source = SERVER.read_text(encoding="utf-8")
        self.assertIn("prerequisites(player, study) && evidenceSatisfied(player, study)", source)
        self.assertIn('knows(player, "material_observation")', source)


if __name__ == "__main__":
    unittest.main()
