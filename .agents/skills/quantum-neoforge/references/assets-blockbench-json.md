# Assets, Blockbench, JSON Models & Translations

## Resource Layout

- **Models & Blockstates:** `src/main/resources/assets/quantumindustries/`
  - `blockstates/*.json`
  - `models/block/*.json`, `models/item/*.json`
  - `items/*.json` — **Mandatory 1.21.4+ / 26.1.2 Item Model Definitions** (`{"model":{"type":"minecraft:model","model":"quantumindustries:item/..."}}`). Every new item/block-item needs one or it renders missing in inventory/hand.
  - `geckolib/models/*.json`, `geckolib/animations/*.json` (GeckoLib `5.5.2`)
  - `textures/block/*.png`, `textures/item/*.png`, `textures/gui/*.png`
- **Data:** `src/main/resources/data/quantumindustries/` (`recipe/`, `loot_table/`, `neoforge/`, `worldgen/`).
- **Blockbench source files:** `design/*.bbmodel` (and `design/assay-v2/`, `design/mechanical/`). `build.gradle` explicitly excludes `**/*.bbmodel` from packaged resources.
- **Generated resources:** `src/generated/resources` (cache excluded from packaging).

## Project Rules & Complementary Skills

- **Translations parity:** Update **both** `assets/quantumindustries/lang/en_us.json` and `assets/quantumindustries/lang/pt_br.json` whenever adding/renaming items, blocks, GUIs, or research entries.
- **No missing textures:** Run `./gradlew processResources` (or full `build`) and ensure zero `Missing texture` / broken model warnings.
- **Blockbench workflow:** Use the `blockbench` MCP and `blockbench-model-architect` skill when creating or editing `.bbmodel` files (verify written files on disk per the `save_project` pitfall).
- **Bulk JSON/recipes/loot tables:** Follow `datapack-json-automator` patterns, then run the repo's Python contract tests (`tests/test_assay_resources.py`, `tests/test_mechanical_resources.py`, `tests/test_era2_features.py`, etc.) to validate JSON alignment.
- **Do not redistribute third-party assets:** `design/emendatus_assets/` and `design/vanilla_textures/` are gitignored local references only.
