import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"
DATA = ROOT / "src/main/resources/data/quantumindustries"


class VaultAndGuidebookTest(unittest.TestCase):
    def test_vault_and_guidebook_registrations_exist(self):
        mod_blocks = (ROOT / "src/main/java/br/com/quantumindustries/core/ModBlocks.java").read_text(encoding="utf-8")
        mod_items = (ROOT / "src/main/java/br/com/quantumindustries/ModItems.java").read_text(encoding="utf-8")
        mod_menus = (ROOT / "src/main/java/br/com/quantumindustries/core/ModMenuTypes.java").read_text(encoding="utf-8")
        client_menus = (ROOT / "src/main/java/br/com/quantumindustries/client/ClientMenus.java").read_text(encoding="utf-8")

        self.assertIn('"quantum_vault"', mod_blocks)
        self.assertIn("QUANTUM_VAULT_ENTITY", mod_blocks)
        self.assertIn("QUANTUM_VAULT", mod_items)
        self.assertIn('"quantum_guidebook"', mod_items)
        self.assertIn('"quantum_guidebook_menu"', mod_menus)
        self.assertIn("QuantumGuidebookScreen::new", client_menus)
        self.assertIn("QuantumVaultRenderer::new", client_menus)

    def test_vault_drawer_and_hologram_renderer_exist(self):
        be = (ROOT / "src/main/java/br/com/quantumindustries/block/entity/QuantumVaultBlockEntity.java").read_text(encoding="utf-8")
        renderer = (ROOT / "src/client/java/br/com/quantumindustries/client/renderer/QuantumVaultRenderer.java").read_text(encoding="utf-8")
        self.assertIn("MAX_CAPACITY = 512", be)
        self.assertIn("insert(", be)
        self.assertIn("extract(", be)
        self.assertIn("drawHologramBeam", renderer)
        self.assertIn("state.item.submit", renderer)

    def test_guidebook_catalog_And_obfuscation_contract(self):
        catalog = (ROOT / "src/main/java/br/com/quantumindustries/research/GuidebookCatalog.java").read_text(encoding="utf-8")
        screen = (ROOT / "src/main/java/br/com/quantumindustries/client/QuantumGuidebookScreen.java").read_text(encoding="utf-8")
        spawner = (ROOT / "src/main/java/br/com/quantumindustries/world/VaultImpactSpawner.java").read_text(encoding="utf-8")

        self.assertIn('"intro_vault"', catalog)
        self.assertIn('"material_observation"', catalog)
        self.assertIn('"electric_alloy_smelting"', catalog)
        self.assertIn("????", screen)
        self.assertIn("gui.quantumindustries.text.encrypted_record", screen)
        self.assertIn("gui.quantumindustries.text.how_to_operate", screen)
        self.assertIn("isVaultSpawned", spawner)
        self.assertIn("markVaultSpawned", spawner)

    def test_industrial_chapters_match_research_gates_and_localization(self):
        catalog = (ROOT / "src/main/java/br/com/quantumindustries/research/GuidebookCatalog.java").read_text(encoding="utf-8")
        menu = (ROOT / "src/main/java/br/com/quantumindustries/menu/QuantumGuidebookMenu.java").read_text(encoding="utf-8")
        screen = (ROOT / "src/main/java/br/com/quantumindustries/client/QuantumGuidebookScreen.java").read_text(encoding="utf-8")
        expected = [
            ('compact_gearbox', 'mechanical_transmission', 1, 'COMPACT_GEARBOX', 'metal_sheets'),
            ('mechanical_conveyor', 'mechanical_transmission', 1, 'MECHANICAL_CONVEYOR', 'metal_sheets'),
            ('industrial_circuit_switch', 'load_protection', 2, 'INDUSTRIAL_CIRCUIT_SWITCH', None),
            ('electromagnetic_generator', 'electromagnetic_induction', 2, 'ELECTROMAGNETIC_GENERATOR', None),
            ('electric_motor', 'electromechanical_forming', 2, 'ELECTRIC_MOTOR', None),
            ('steam_boiler', 'fluid_hydrostatics', 2, 'STEAM_BOILER', 'thermal_annealing'),
            ('steam_engine', 'fluid_hydrostatics', 2, 'STEAM_ENGINE', 'thermal_annealing'),
            ('thermoelectric_generator', 'electric_alloy_smelting', 2, 'THERMOELECTRIC_GENERATOR', None),
        ]
        for chapter, study, era, item, extra in expected:
            suffix = f', "{extra}"' if extra else ''
            self.assertIn(f'machine("{chapter}", "{study}", {era}, () -> new ItemStack(ModItems.{item}.get()){suffix})', catalog)
            for locale in ("pt_br.json", "en_us.json"):
                lang = json.loads((ASSETS / "lang" / locale).read_text(encoding="utf-8"))
                for field in ("title", "subtitle", "paragraph_0", "paragraph_1", "operation"):
                    self.assertTrue(lang.get(f"guidebook.quantumindustries.{chapter}.{field}"), (locale, chapter, field))
        self.assertIn("chapter.requiredStudies().stream().allMatch", menu)
        self.assertIn("chapter.requiredStudies().stream()", screen)
        self.assertIn("additionalStudies.stream()", catalog)

    def test_vault_and_guidebook_assets_and_translations(self):
        for rel in [
            "blockstates/quantum_vault.json",
            "models/block/quantum_vault.json",
            "models/item/quantum_vault.json",
            "models/item/quantum_guidebook.json",
            "items/quantum_vault.json",
            "items/quantum_guidebook.json",
            "textures/block/quantum_vault_base.png",
            "textures/block/quantum_vault_core.png",
            "textures/item/quantum_guidebook.png",
        ]:
            path = ASSETS / rel
            self.assertTrue(path.is_file(), f"Missing asset: {rel}")

        loot = DATA / "loot_table/blocks/quantum_vault.json"
        self.assertTrue(loot.is_file(), "Missing quantum_vault loot table")

        for lang_file in ["pt_br.json", "en_us.json"]:
            lang = json.loads((ASSETS / "lang" / lang_file).read_text(encoding="utf-8"))
            for key in [
                "block.quantumindustries.quantum_vault",
                "item.quantumindustries.quantum_vault",
                "item.quantumindustries.quantum_guidebook",
                "message.quantumindustries.vault_landed",
                "message.quantumindustries.vault_opened",
            ]:
                self.assertIn(key, lang, f"Missing {key} in {lang_file}")


if __name__ == "__main__":
    unittest.main()
