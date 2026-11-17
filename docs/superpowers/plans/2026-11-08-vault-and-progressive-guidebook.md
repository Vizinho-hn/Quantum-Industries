# Vault & Progressive Guidebook (`????`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a crashed `Quantum Vault` crater near world spawn on first world join that grants a progressive `Quantum Guidebook` (`quantum_guidebook`) with Botania-style `????` obfuscation for unlearned machines and detailed operational guides for unlocked machines (without showing crafting recipes).

**Architecture:**
1. Extend `WorldResearchData` with `vaultSpawned` boolean state and add `VaultImpactSpawner` (`PlayerLoggedInEvent`) to generate a 5x5 scorched crater with a `QuantumVaultBlock` at the surface near spawn once per world.
2. Implement `QuantumVaultBlock`, `QuantumGuidebookItem`, `QuantumGuidebookMenu` (syncing unlocked `ResearchCatalog` studies via `ContainerData`), `GuidebookCatalog` (intro + 16 machine/system chapters across Era I and Era II), and `QuantumGuidebookScreen` (`????` obfuscation when locked, full machine manual when unlocked).
3. Add 3D block/item models, textures, loot table, and `pt_br` / `en_us` translations, plus automated Python contract tests.

**Tech Stack:** Java 25, NeoForge 26.1.2.125 (Minecraft 26.1.2), Gradle 9.2.1, Python `unittest`.

---

### Task 1: WorldResearchData `vaultSpawned` Flag & `GuidebookCatalog`

**Files:**
- Modify: `src/main/java/br/com/quantumindustries/research/WorldResearchData.java`
- Create: `src/main/java/br/com/quantumindustries/research/GuidebookCatalog.java`

- [ ] **Step 1: Add `vaultSpawned` persistence to `WorldResearchData`**
  - Add `Codec.BOOL.optionalFieldOf("vault_spawned", false).forGetter(WorldResearchData::isVaultSpawned)` to `WorldResearchData.CODEC`.
  - Add `private boolean vaultSpawned;` field, `public boolean isVaultSpawned()`, and `public void markVaultSpawned()` (which sets `this.vaultSpawned = true; setDirty();`).
  - Keep the no-arg constructor `public WorldResearchData()` delegating with `false`.

- [ ] **Step 2: Create `GuidebookCatalog.java`**
  - Create `br.com.quantumindustries.research.GuidebookCatalog` with `public record Chapter(String id, String studyKey, int era, String title, String subtitle, Supplier<ItemStack> icon, List<String> paragraphs, String operationSummary)`.
  - Include Entry 0 (`"intro_vault"`, `studyKey = null`, `era = 0`, title `"O Vault e a Mesa de Pesquisa"`) which is always readable and explains:
    - Why the Vault fell and why all advanced chapters appear as `????`.
    - How to start: build the **Mesa de Pesquisa (Caderno/Bancada)** and the **Analisador de Materiais** to inspect wood, stone, and copper, unlocking new chapters automatically.
  - Include 16 machine/system operational chapters mapped to valid keys in `ResearchCatalog.STUDIES` (`material_observation`, `manual_forming`, `alloy_metallurgy`, `mechanical_transmission`, `hydraulic_power`, `mineral_beneficiation`, `copper_wire`, `manual_dynamo_design`, `galvanic_storage`, `load_protection`, `thermal_annealing`, `wire_drawing`, `electromechanical_forming`, `electric_alloy_smelting`, `fluid_hydrostatics`, `pneumatic_transport`, `chemical_leaching`).
  - Validate in a `static` block that every non-null `studyKey` exists in `ResearchCatalog.find(studyKey)`.

---

### Task 2: `QuantumVaultBlock`, `QuantumGuidebookItem`, `QuantumGuidebookMenu`, and Crater Spawner

**Files:**
- Create: `src/main/java/br/com/quantumindustries/block/QuantumVaultBlock.java`
- Create: `src/main/java/br/com/quantumindustries/item/QuantumGuidebookItem.java`
- Create: `src/main/java/br/com/quantumindustries/menu/QuantumGuidebookMenu.java`
- Create: `src/main/java/br/com/quantumindustries/world/VaultImpactSpawner.java`
- Modify: `src/main/java/br/com/quantumindustries/core/ModBlocks.java`
- Modify: `src/main/java/br/com/quantumindustries/ModItems.java`
- Modify: `src/main/java/br/com/quantumindustries/core/ModMenuTypes.java`
- Modify: `src/main/java/br/com/quantumindustries/core/ModCreativeTabs.java`
- Modify: `src/main/java/br/com/quantumindustries/QuantumIndustries.java`

- [ ] **Step 1: Create `QuantumGuidebookMenu.java`**
  - Extend `AbstractContainerMenu`.
  - Sync `ResearchCatalog.STUDIES.size()` integer slots via `ContainerData`, where `get(i)` returns `1` if `WorldResearchData.get(serverLevel).knows(player, i)` is true and `0` otherwise.
  - Expose `public boolean knows(int studyIndex)` and `public boolean isChapterUnlocked(GuidebookCatalog.Chapter chapter)`.

- [ ] **Step 2: Create `QuantumGuidebookItem.java`**
  - Extend `Item`. Override `use(Level level, Player player, InteractionHand hand)`: on server (`ServerLevel`), call `player.openMenu(new SimpleMenuProvider((id, inv, p) -> new QuantumGuidebookMenu(id, inv), Component.translatable("item.quantumindustries.quantum_guidebook")))`. Return `InteractionResult.SUCCESS`.

- [ ] **Step 3: Create `QuantumVaultBlock.java`**
  - Extend `Block` with `BooleanProperty OPEN = BlockStateProperties.OPEN`.
  - Override `useWithoutItem` and `useItemOn`: on server, if the player does not have `ModItems.QUANTUM_GUIDEBOOK.get()` in inventory, grant 1 copy (`player.getInventory().placeItemBackInInventory(...)`), set blockstate `OPEN = true`, play vault open sound, and send `message.quantumindustries.vault_opened`. Also open the guidebook menu so the player immediately sees the book UI on interaction.

- [ ] **Step 4: Create `VaultImpactSpawner.java`**
  - Listen to `PlayerEvent.PlayerLoggedInEvent`.
  - If `event.getEntity().level() instanceof ServerLevel serverLevel` (Overworld) and `!WorldResearchData.get(serverLevel).isVaultSpawned()`:
    - Pick a target surface position `~12` blocks East and `+6` blocks South of the player's position using `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES`.
    - Carve a small 5x5 scorched impact crater (`Blocks.COBBLED_DEEPSLATE`, `Blocks.BLACKSTONE`, `Blocks.BASALT`, `Blocks.MAGMA_BLOCK`, plus 2 `Blocks.FIRE` above magma).
    - Place `ModBlocks.QUANTUM_VAULT.get().defaultBlockState()` at the center of the crater.
    - Call `WorldResearchData.get(serverLevel).markVaultSpawned()` and send `Component.translatable("message.quantumindustries.vault_landed")` to the player.

- [ ] **Step 5: Register in `ModBlocks`, `ModItems`, `ModMenuTypes`, `ModCreativeTabs`, and `QuantumIndustries`**
  - Register `QUANTUM_VAULT` in `ModBlocks` and `ModItems`, `QUANTUM_GUIDEBOOK` in `ModItems`, and `QUANTUM_GUIDEBOOK_MENU` in `ModMenuTypes`.
  - Add `QUANTUM_VAULT` and `QUANTUM_GUIDEBOOK` to `ModCreativeTabs`.
  - Register `VaultImpactSpawner::onPlayerLogin` on `NeoForge.EVENT_BUS` in `QuantumIndustries`.

---

### Task 3: Client UI (`QuantumGuidebookScreen`) & Assets/Translations

**Files:**
- Create: `src/main/java/br/com/quantumindustries/client/QuantumGuidebookScreen.java`
- Modify: `src/main/java/br/com/quantumindustries/client/ClientMenus.java`
- Create: `src/main/resources/assets/quantumindustries/blockstates/quantum_vault.json`
- Create: `src/main/resources/assets/quantumindustries/models/block/quantum_vault.json`
- Create: `src/main/resources/assets/quantumindustries/models/item/quantum_vault.json`
- Create: `src/main/resources/assets/quantumindustries/items/quantum_vault.json`
- Create: `src/main/resources/assets/quantumindustries/models/item/quantum_guidebook.json`
- Create: `src/main/resources/assets/quantumindustries/items/quantum_guidebook.json`
- Create: `src/main/resources/data/quantumindustries/loot_table/blocks/quantum_vault.json`
- Modify: `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`
- Modify: `src/main/resources/assets/quantumindustries/lang/pt_br.json`
- Modify: `src/main/resources/assets/quantumindustries/lang/en_us.json`
- Create: `tests/test_vault_and_guidebook.py`

- [ ] **Step 1: Implement `QuantumGuidebookScreen.java` and register in `ClientMenus.java`**
  - Extend `AbstractContainerScreen<QuantumGuidebookMenu>`.
  - Render a clean two-column technical codex using `ResearchTheme`:
    - Top bar: title `"CÓDICE QUANTUM INDUSTRIES — MANUAL DE MÁQUINAS"` + filter tabs (`"Início"`, `"Era I"`, `"Era II"`, `"Todos"`) + unlocked counter (`X / Y decodificados`).
    - Left column: scrollable chapter list. Unlocked chapters show their item icon + title; locked chapters show a lock icon + `"????"`.
    - Right column:
      - If the selected chapter is **unlocked**: render the icon, title, subtitle, wrapped paragraphs explaining how the machine works, and a highlighted `"COMO OPERAR NA PRÁTICA"` box at the bottom.
      - If the selected chapter is **locked**: render `"REGISTRO CRIPTOGRAFADO — ????"`, multiple lines of `"???? ???????? ???? ?????? ????"` in muted/gold ink, and a box telling the player to unlock the corresponding research on the Research Table to decipher this machine's manual.

- [ ] **Step 2: Create 3D Models, Blockstates, Item Definitions, Loot Table, and Translations**
  - Create a 3D multi-element block model for `quantum_vault` (dark metallic body, reinforced corners, glowing cyan core) and a 3D/layered model for `quantum_guidebook`.
  - Add `quantumindustries:quantum_vault` to `mineable/pickaxe.json` and create its block loot table.
  - Add `pt_br.json` and `en_us.json` entries for `block.quantumindustries.quantum_vault`, `item.quantumindustries.quantum_vault`, `item.quantumindustries.quantum_guidebook`, `message.quantumindustries.vault_landed`, and `message.quantumindustries.vault_opened`.

- [ ] **Step 3: Write `tests/test_vault_and_guidebook.py` and run verification**
  - Verify all new assets, translations, registrations, and `GuidebookCatalog` structure.
  - Run `python3 -m unittest discover -s tests` and `./gradlew classes verifyLegacyResearch --no-daemon`.
