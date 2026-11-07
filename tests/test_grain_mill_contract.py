"""Static contracts for the Era I grain-mill vertical slice.

These tests validate registrations, assets, the vanilla food balance and machine
invariants. They do not replace compilation and actual Minecraft playtesting.
"""
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/br/com/quantumindustries"
ASSETS = ROOT / "src/main/resources/assets/quantumindustries"
DATA = ROOT / "src/main/resources/data/quantumindustries"


def source(path):
    return (JAVA / path).read_text(encoding="utf-8")


def read_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


class GrainMillContracts(unittest.TestCase):
    def test_block_entity_registration_and_creative_tab(self):
        items = source("ModItems.java")
        blocks = source("core/ModBlocks.java")
        creative = source("core/ModCreativeTabs.java")
        for name in ("MANUAL_GRAIN_MILL", "FLOUR", "BREAD_DOUGH", "RUSTIC_BREAD"):
            self.assertIn(name, items)
            self.assertIn(f"ModItems.{name}.get()", creative)
        self.assertIn('registerBlock("manual_grain_mill"', blocks)
        self.assertIn('BLOCK_ENTITIES.register("manual_grain_mill"', blocks)
        self.assertIn("ManualGrainMillBlockEntity::new", blocks)

    def test_grain_mill_operation_persistence_and_drops(self):
        block = source("block/ManualGrainMillBlock.java")
        entity = source("block/entity/ManualGrainMillBlockEntity.java")
        self.assertIn("Items.WHEAT", block)
        self.assertIn("Items.WHEAT", entity)
        self.assertIn("public static final int STROKES = 4", entity)
        self.assertIn("stack.shrink(1)", entity.replace("held.shrink(1)", "stack.shrink(1)"))
        self.assertIn("input.shrink(1)", entity)
        self.assertIn("output.grow(1)", entity)
        self.assertIn("output.getCount() >= output.getMaxStackSize()", entity)
        self.assertIn('out.store("input", ItemStack.CODEC, input)', entity)
        self.assertIn('out.store("output", ItemStack.CODEC, output)', entity)
        self.assertIn('out.putInt("progress", progress)', entity)
        self.assertIn("Block.popResource(server, pos", entity)
        self.assertIn("level instanceof ServerLevel", entity)
        self.assertIn("player.isShiftKeyDown()", block)

    def test_recipes_and_balance(self):
        grinder = read_json(DATA / "recipe/manual_grain_mill.json")
        self.assertEqual(grinder["craft"], "manual_grain_mill")
        foundations = source("recipe/FoundationRecipe.java")
        self.assertIn('new Pattern("material_observation", ModItems.MANUAL_GRAIN_MILL', foundations)
        dough = read_json(DATA / "recipe/bread_dough.json")
        self.assertEqual(dough["type"], "minecraft:crafting_shapeless")
        self.assertEqual(dough["category"], "misc")
        self.assertEqual(dough["ingredients"].count("quantumindustries:flour"), 2)
        self.assertIn("minecraft:water_bucket", dough["ingredients"])
        self.assertEqual(dough["result"]["id"], "quantumindustries:bread_dough")
        bread = read_json(DATA / "recipe/rustic_bread.json")
        self.assertEqual(bread["type"], "minecraft:smelting")
        self.assertEqual(bread["ingredient"], "quantumindustries:bread_dough")
        self.assertEqual(bread["result"]["id"], "quantumindustries:rustic_bread")
        items = source("ModItems.java")
        self.assertIn(".nutrition(5).saturationModifier(0.6F)", items)
        self.assertNotIn('registerItem("bread",', items)

    def test_models_blockstates_loot_and_localization(self):
        blockstate = read_json(ASSETS / "blockstates/manual_grain_mill.json")
        self.assertEqual(set(blockstate["variants"]), {
            "facing=north", "facing=east", "facing=south", "facing=west"
        })
        model = read_json(ASSETS / "models/block/manual_grain_mill.json")
        self.assertGreaterEqual(len(model["elements"]), 8)
        for element in model["elements"]:
            for start, end in zip(element["from"], element["to"]):
                self.assertTrue(0 <= start < end <= 16, element)
        loot = read_json(DATA / "loot_table/blocks/manual_grain_mill.json")
        self.assertEqual(loot["pools"][0]["entries"][0]["name"],
                         "quantumindustries:manual_grain_mill")
        for name in ("manual_grain_mill", "flour", "bread_dough", "rustic_bread"):
            self.assertTrue((ASSETS / "models/item" / f"{name}.json").is_file())
            item_model = read_json(ASSETS / "items" / f"{name}.json")
            self.assertEqual(item_model["model"]["model"],
                             f"quantumindustries:item/{name}")
        for lang in ("pt_br", "en_us"):
            loc = read_json(ASSETS / "lang" / f"{lang}.json")
            self.assertIn("block.quantumindustries.manual_grain_mill", loc)
            for name in ("flour", "bread_dough", "rustic_bread"):
                self.assertIn(f"item.quantumindustries.{name}", loc)
            self.assertIn("gui.quantumindustries.grain_mill.status", loc)


if __name__ == "__main__":
    unittest.main()
