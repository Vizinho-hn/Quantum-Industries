#!/usr/bin/env python3
"""Static F0 contract checks (not a substitute for Minecraft runtime tests)."""
import json
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
        self.assertIn('Codec.BOOL.optionalFieldOf("conductivity", false)', source)
        self.assertIn('"research"', source)
        self.assertIn("getDataStorage().computeIfAbsent(TYPE)", source)
        self.assertIn("setDirty()", source)
        self.assertIn('Codec.BOOL.optionalFieldOf("copper_wire", false)', source)

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
        self.assertNotIn("addSlot(", menu)
        self.assertIn("new SimpleContainerData(DATA_SIZE)", menu)
        self.assertIn("clickMenuButton", menu)
        self.assertIn("prepareSpend(player.getInventory()", menu)
        self.assertIn("payment.commit()", menu)
        self.assertIn("payment.rollback()", menu)
        self.assertIn('output.putInt("progress", legacyProgress)', entity)
        self.assertIn('output.store("slot_" + i, ItemStack.CODEC, stack)', entity)

    def test_recipe_requires_research_on_server(self):
        recipe = read("src/main/java/br/com/quantumindustries/recipe/FoundationRecipe.java")
        self.assertIn('new Pattern("copper_wire", ModItems.COPPER_WIRE', recipe)
        self.assertIn('ResearchCraftingAccess.knows(player, "conductivity")', recipe)
        self.assertIn('crafter != null && unlocked(crafter) && craft.fits(grid)', recipe)
        policy = read("src/main/java/br/com/quantumindustries/recipe/ResearchCraftingAccess.java")
        self.assertIn("instanceof ServerLevel", policy)
        self.assertIn("player != null && player.level() == level", policy)
        menu = read("src/main/java/br/com/quantumindustries/menu/ResearchTableMenu.java")
        self.assertIn("!stillValid(player)", menu)
        self.assertIn("WorldResearchData.get(serverLevel)", menu)

    def test_f0_screen_does_not_fake_unlocked_research(self):
        screen = read("src/main/java/br/com/quantumindustries/client/ResearchTableScreen.java")
        self.assertIn("menu.complete(id)", screen)
        self.assertNotIn("unlockConductivity(", screen)
        self.assertNotIn("TITLES =", screen)
        self.assertIn("ResearchTheme", screen)
        self.assertNotIn("AMOSTRA", screen)
        self.assertNotIn("SONDA", screen)
        self.assertNotIn("SAIDA", screen)

    def test_wire_item_definition_points_to_textured_3d_model(self):
        assets = ROOT / "src/main/resources/assets/quantumindustries"
        item = json.loads((assets / "items/copper_wire.json").read_text(encoding="utf-8"))
        self.assertEqual(item["model"]["model"], "quantumindustries:item/copper_wire")
        model = json.loads((assets / "models/item/copper_wire.json").read_text(encoding="utf-8"))
        self.assertTrue(model["elements"])
        self.assertTrue((assets / "textures/item/copper_wire.png").is_file())

    def test_core_resources(self):
        assets = ROOT / "src/main/resources/assets/quantumindustries"
        item = json.loads((assets / "items/primitive_mechanical_core.json").read_text())
        self.assertEqual(item["model"]["model"], "quantumindustries:item/primitive_mechanical_core")
        model = json.loads((assets / "models/item/primitive_mechanical_core.json").read_text())
        texture = model["textures"]["materials"].split(":")[1]
        self.assertTrue((assets / "textures" / (texture + ".png")).is_file())
        self.assertGreater(len(model["elements"]), 1)
        self.assertTrue({"gui", "ground", "firstperson_righthand"} <= model["display"].keys())
        for element in model["elements"]:
            for start, end in zip(element["from"], element["to"]):
                self.assertTrue(0 <= start < end <= 16)
        for locale in ("en_us", "pt_br"):
            labels = json.loads((assets / "lang" / (locale + ".json")).read_text(encoding="utf-8"))
            self.assertTrue(labels["item.quantumindustries.primitive_mechanical_core"])

if __name__ == "__main__":
    unittest.main()
