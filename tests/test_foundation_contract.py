#!/usr/bin/env python3
"""Static F0 contract checks (not a substitute for Minecraft runtime tests)."""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src"
MAIN = JAVA / "main/java/br/com/quantumindustries"

def read(relative):
    return (ROOT / relative).read_text(encoding="utf-8")

class FoundationContractTest(unittest.TestCase):
    def test_only_one_research_screen(self):
        files = sorted(p.relative_to(ROOT).as_posix() for p in JAVA.rglob("ResearchTableScreen.java"))
        self.assertEqual(files, ["src/main/java/br/com/quantumindustries/client/ResearchTableScreen.java"])

    def test_exactly_one_research_screen_registration(self):
        registrations = []
        for path in JAVA.rglob("*.java"):
            if "event.register(ModMenuTypes.RESEARCH_TABLE_MENU.get()" in path.read_text(encoding="utf-8"):
                registrations.append(path.relative_to(ROOT).as_posix())
        self.assertEqual(registrations, ["src/main/java/br/com/quantumindustries/client/ClientMenus.java"])

    def test_world_save_codec_keeps_legacy_conductivity(self):
        source = read("src/main/java/br/com/quantumindustries/research/WorldResearchData.java")
        self.assertIn('Codec.BOOL.fieldOf("conductivity")', source)
        self.assertIn('"research"', source)
        self.assertIn("getDataStorage().computeIfAbsent(TYPE)", source)
        self.assertIn("setDirty()", source)

    def test_registry_ids_stable(self):
        blocks = read("src/main/java/br/com/quantumindustries/core/ModBlocks.java")
        menus = read("src/main/java/br/com/quantumindustries/core/ModMenuTypes.java")
        items = read("src/main/java/br/com/quantumindustries/ModItems.java")
        self.assertIn('registerBlock("scientific_analyzer"', blocks)
        self.assertIn('register("research_table"', blocks)
        self.assertIn('register("research_table_menu"', menus)
        self.assertIn("registerSimpleBlockItem(RESEARCH_TABLE)", items)
        self.assertIn('"copper_wire"', items)
        self.assertIn('"research_data"', items)
        self.assertIn('"test_probes"', items)

    def test_menu_slots_and_save_keys_stable(self):
        menu = read("src/main/java/br/com/quantumindustries/menu/ResearchTableMenu.java")
        entity = read("src/main/java/br/com/quantumindustries/block/entity/ResearchTableBlockEntity.java")
        for needle in ("Slot(inventory, 0, 21, 72)", "Slot(inventory, 1, 21, 108)",
                       "Slot(inventory, 2, 66, 90)", "new SimpleContainerData(4)"):
            self.assertIn(needle, menu)
        self.assertIn('output.putInt("progress", progress)', entity)
        self.assertIn('output.store("slot_" + i, ItemStack.CODEC, stack)', entity)

    def test_f0_screen_does_not_fake_unlocked_research(self):
        screen = read("src/main/java/br/com/quantumindustries/client/ResearchTableScreen.java")
        self.assertIn("menu.hasConductivity()", screen)
        self.assertNotIn("unlockConductivity(", screen)
        self.assertNotIn("TITLES =", screen)
        self.assertIn("DEEP", screen)

if __name__ == "__main__":
    unittest.main()
