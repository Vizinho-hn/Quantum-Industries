import pathlib
import json
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
BASE=ROOT/"src/main/resources"
JAVA=ROOT/"src/main/java/br/com/quantumindustries"
BLOCKS=("pneumatic_tube","bellows_ejector","gravitational_sorter",
        "fluid_pipe","glass_pipe","peristaltic_pump","settling_vat")
STUDIES=("fluid_hydrostatics","pneumatic_transport","peristaltic_mechanics")

class LogisticsContracts(unittest.TestCase):
    def test_models_and_crafts(self):
        patterns=(JAVA/"recipe/FoundationRecipe.java").read_text()
        for name in BLOCKS:
            with self.subTest(name=name):
                for part in (f"assets/quantumindustries/items/{name}.json",
                            f"assets/quantumindustries/blockstates/{name}.json",
                            f"assets/quantumindustries/models/block/{name}.json",
                            f"data/quantumindustries/recipe/{name}.json",
                            f"data/quantumindustries/loot_table/blocks/{name}.json"):
                    f=BASE/part
                    self.assertTrue(f.exists(),f)
                    json.loads(f.read_text())
                self.assertIn("ModItems."+name.upper(),patterns)
    def test_research_links(self):
        research=(JAVA/"research/ResearchCatalog.java").read_text()
        for study in STUDIES:self.assertIn('new Study("'+study+'"',research)
    def test_no_fake_fluid_or_item_generation(self):
        transfer=(JAVA/"fluid/FluidTransfer.java").read_text()
        self.assertIn("tx.commit()",transfer)
        self.assertIn("accepted!=taken",transfer)
        tube=(JAVA/"block/entity/PneumaticTubeBlockEntity.java").read_text()
        self.assertIn('out.store("packet"',tube)
        self.assertIn("preRemoveSideEffects",tube)
    def test_research_recipes_language(self):
        for lang in ("pt_br","en_us"):
            data=json.loads((BASE/f"assets/quantumindustries/lang/{lang}.json").read_text())
            for block in BLOCKS:self.assertIn("block.quantumindustries."+block,data)

if __name__=="__main__":unittest.main()
