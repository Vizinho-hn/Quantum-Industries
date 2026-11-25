"""Contracts for QES, QTS, Era I/II branch separation, Electric Furnace, and scientific documentation."""
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/br/com/quantumindustries"
CLIENT = ROOT / "src/client/java/br/com/quantumindustries"
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"
DATA = ROOT / "src/main/resources/data/quantumindustries"


class PhysicsFoundationsTest(unittest.TestCase):
    def test_qts_and_qes_core_and_checks_exist(self):
        expected = [
            JAVA / "energy/PhysicsEnergyContract.java",
            JAVA / "energy/qes/ElectricalMaterial.java",
            JAVA / "energy/qes/ElectricalMath.java",
            JAVA / "energy/qes/ElectricalNodeState.java",
            JAVA / "energy/qes/ElectricalNetworkState.java",
            JAVA / "energy/qes/ElectricalSolver.java",
            JAVA / "thermal/ThermalMaterial.java",
            JAVA / "thermal/ThermalMath.java",
            JAVA / "thermal/ThermalBody.java",
            JAVA / "thermal/ThermalSnapshot.java",
            JAVA / "thermal/ThermalInspectable.java",
            JAVA / "thermal/ThermalParticipant.java",
            JAVA / "thermal/ThermalNetwork.java",
            JAVA / "inspection/ThermalInspectionProvider.java",
            ROOT / "tests/br/com/quantumindustries/thermal/ThermalSystemCheck.java",
            ROOT / "tests/br/com/quantumindustries/energy/ElectricalSystemCheck.java",
        ]
        for path in expected:
            self.assertTrue(path.is_file(), f"Missing physics file: {path}")

        gradle = (ROOT / "build.gradle").read_text(encoding="utf-8")
        self.assertIn("verifyThermalSystem", gradle)
        self.assertIn("verifyElectricalSystem", gradle)
        self.assertIn("verifyCableTelemetryWindow", gradle)

    def test_era_two_branches_and_codex_chapters(self):
        catalog = (JAVA / "research/ResearchCatalog.java").read_text(encoding="utf-8")
        self.assertIn("ERA_TWO_BRANCHES", catalog)
        self.assertIn("enum EraTwoTrack", catalog)
        self.assertIn('new Study("heat_transfer"', catalog)
        self.assertIn('new Study("joule_heating"', catalog)

        guidebook = (JAVA / "research/GuidebookCatalog.java").read_text(encoding="utf-8")
        for chapter in (
            "temperature_thermal_equilibrium",
            "heat_thermal_capacity",
            "first_law_thermodynamics",
            "electric_charges",
            "current_and_resistance",
            "ohms_law",
            "electrical_power",
            "joule_effect",
            "energy_conversions",
            "electric_furnace",
            "electrical_network_analyzer",
        ):
            self.assertIn(f'"{chapter}"', guidebook)

        self.assertIn("era2_thermal_sciences", catalog)
        self.assertIn("era2_electricity_electromagnetism", catalog)
        tree_view = (JAVA / "client/ResearchTreeView.java").read_text(encoding="utf-8")
        self.assertIn("ERA_TWO_BRANCHES", tree_view)

    def test_electric_furnace_registration_and_resources(self):
        blocks = (JAVA / "core/ModBlocks.java").read_text(encoding="utf-8")
        items = (ROOT / "src/main/java/br/com/quantumindustries/ModItems.java").read_text(encoding="utf-8")
        menus = (JAVA / "core/ModMenuTypes.java").read_text(encoding="utf-8")
        client_menus = (JAVA / "client/ClientMenus.java").read_text(encoding="utf-8")
        recipes = (JAVA / "recipe/FoundationRecipe.java").read_text(encoding="utf-8")

        self.assertIn("ELECTRIC_FURNACE", blocks)
        self.assertIn("ELECTRIC_FURNACE_ENTITY", blocks)
        self.assertIn("ELECTRIC_FURNACE", items)
        self.assertIn("ELECTRIC_FURNACE_MENU", menus)
        self.assertIn("ElectricFurnaceScreen::new", client_menus)
        self.assertIn('new Pattern("joule_heating", ModItems.ELECTRIC_FURNACE', recipes)

        self.assertTrue((JAVA / "block/ElectricFurnaceBlock.java").is_file())
        self.assertTrue((JAVA / "block/entity/ElectricFurnaceBlockEntity.java").is_file())
        self.assertTrue((JAVA / "menu/ElectricFurnaceMenu.java").is_file())
        self.assertTrue((CLIENT / "client/gui/ElectricFurnaceScreen.java").is_file())
        self.assertTrue((ROOT / "design/mechanical/electric_furnace.bbmodel").is_file())

        for rel in (
            "blockstates/electric_furnace.json",
            "items/electric_furnace.json",
            "models/block/electric_furnace.json",
            "models/block/electric_furnace_lit.json",
            "models/item/electric_furnace.json",
        ):
            path = ASSETS / rel
            self.assertTrue(path.is_file(), path)
            json.loads(path.read_text(encoding="utf-8"))

        recipe_json = json.loads((DATA / "recipe/electric_furnace.json").read_text(encoding="utf-8"))
        self.assertEqual(recipe_json["craft"], "electric_furnace")
        self.assertTrue((DATA / "loot_table/blocks/electric_furnace.json").is_file())

    def test_inspection_snapshot_preserves_legacy_and_qks_prefix(self):
        snap_src = (JAVA / "inspection/InspectionSnapshot.java").read_text(encoding="utf-8")
        # Ensure original 14-arg and 25-arg constructors remain present alongside extended fields.
        self.assertIn("int torqueTenths, List<BlockPos> networkNodes)", snap_src)
        self.assertIn("StopReason stopReason)", snap_src)
        self.assertIn("boolean hasThermal", snap_src)
        self.assertIn("int voltageMv", snap_src)

    def test_scientific_documentation_exists(self):
        for doc in (
            "docs/PHYSICS_SYSTEMS_ARCHITECTURE.md",
            "docs/ELECTRICAL_ENGINE_ARCHITECTURE.md",
            "docs/THERMAL_ENGINE_ARCHITECTURE.md",
            "docs/ELECTRICAL_NETWORK_ANALYZER.md",
        ):
            path = ROOT / doc
            self.assertTrue(path.is_file(), f"Missing doc: {doc}")
            self.assertGreater(len(path.read_text(encoding="utf-8")), 500)


if __name__ == "__main__":
    unittest.main()
