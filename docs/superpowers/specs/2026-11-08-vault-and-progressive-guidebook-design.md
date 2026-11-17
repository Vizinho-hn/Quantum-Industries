# Design Spec: Crashed Quantum Vault & Progressive Guidebook (`????`)

**Date:** 2026-11-08  
**Target Repository:** `/mnt/dados/dev/Quantum-Industries`  
**Status:** Approved

---

## 1. Overview & Motivation
When a player starts a new world in the Quantum Industries modpack, they need an immediate, immersive hook that connects the entire technological progression without giving away recipes prematurely.

Instead of a falling meteor entity, the world begins with a **scorched impact crater** already present near the world spawn (`~12–15` blocks away on the surface). At the center of this smoking crater rests a **Quantum Vault** (`quantum_vault`), a heavy sealed containment unit that survived atmospheric entry.

Interacting with the **Quantum Vault** unseals it and grants the player the **Quantum Codex / Guidebook** (`quantum_guidebook`).

The **Quantum Guidebook** follows a **Lexica Botania (`????`) progression model**:
- **No Crafting Recipes:** Crafting recipes remain strictly in the Research Table / Recipe Book progression. The guidebook focuses exclusively on explaining **how each machine, power system, alloy process, and chemical apparatus works in practice** (inputs, mechanical/electrical operation, outputs, and engineering tips).
- **Chapter 0 (Always Unlocked — "Primeiros Passos & A Queda do Vault"):** Explains the origin of the Vault and instructs the player on their first mandatory goal: building the **Material Analyzer (`scientific_analyzer_tier_1` / `empirical_assay_bench`)** and the **Research Table (`research_table_tier_1` / `research_table`)** to begin empirical assays and unlock technologies.
- **Locked Chapters (`????`):** Every machine and system chapter is bound to a `ResearchCatalog` study key. Until the player completes that study at the Research Table, the chapter title in the index renders as `????` (with a lock icon) and the page body displays obfuscated `????` text along with a hint indicating which research study is required to decipher it.
- **Unlocked Chapters:** As soon as the player completes a study in the Research Table, the corresponding chapter in the Guidebook automatically reveals its true title, icon, and full multi-page operational manual. Once all 28 studies across Era I and Era II are learned, the entire encyclopedia of machines is readable.

---

## 2. Architecture & Components

### 2.1. World Generation / First-Join Crater (`VaultImpactSpawner`)
- **Persistence:** Extend `WorldResearchData` with a boolean field `vaultSpawned` (default `false`, serialized via `Codec.BOOL.optionalFieldOf("vault_spawned", false)` so existing worlds and tests remain 100% backwards-compatible) plus a `BlockPos` or coordinates optional record, or `isVaultSpawned()` / `markVaultSpawned()`.
- **Trigger:** Listen to `PlayerEvent.PlayerLoggedInEvent` on the NeoForge event bus (`NeoForge.EVENT_BUS`). When the first player logs into the Overworld (`ServerLevel` for `Level.OVERWORLD`) and `!researchData.isVaultSpawned()`:
  1. Compute an impact center roughly `+12` blocks East and `+8` blocks South of the player's spawn position (or world spawn), resolving the surface height via `level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos)`.
  2. Carve a shallow 5x5 scorched impact basin (radius 3):
     - Outer rim / floor blocks replaced with a mix of `Blocks.BLACKSTONE`, `Blocks.COBBLED_DEEPSLATE`, `Blocks.BASALT`, and `Blocks.MAGMA_BLOCK`.
     - Place `Blocks.CAMPFIRE` (unlit or buried under magma/fire for smoke) or `Blocks.FIRE` on 2–3 magma/netherrack spots so the crater visibly smokes and burns on arrival.
  3. Place `ModBlocks.QUANTUM_VAULT.get().defaultBlockState()` at the exact center of the crater on solid foundation.
  4. Mark `researchData.markVaultSpawned()` and save.
  5. Send a subtle atmospheric chat message to the player (`Component.translatable("message.quantumindustries.vault_landed")`).

### 2.2. Block: `QuantumVaultBlock` (`quantum_vault`)
- **Registry ID:** `quantumindustries:quantum_vault` (Block + BlockItem in `ModBlocks` and `ModItems`, added to `ModCreativeTabs`).
- **Properties:** Heavy metallic vault (`MapColor.COLOR_CYAN`, strength `4.0F`, `requiresCorrectToolForDrops()`, light level `5` from its glowing core).
- **Blockstate & 3D Model:**
  - Boolean property `OPENED` (`BlockStateProperties.OPEN`).
  - Detailed multi-element 3D cubic model (`models/block/quantum_vault.json`) featuring a reinforced dark-steel octagonal/chamfered containment hull, brass/gold corner bumpers, and a glowing cyan quantum seal on the top/front face.
- **Interaction (`useWithoutItem` / `useItemOn`):**
  - On right-click: if the player does not currently have a `quantum_guidebook` in their inventory, give them `new ItemStack(ModItems.QUANTUM_GUIDEBOOK.get())`, set `OPENED = true`, play `SoundEvents.IRON_DOOR_OPEN` + `SoundEvents.BEACON_ACTIVATE`, and display a status message.
  - If the player already has the guidebook, open the guidebook menu directly (or inform them they already hold the Codex).
- **Loot Table:** Drops `quantumindustries:quantum_vault` (and if broken before opening, also drops `quantumindustries:quantum_guidebook`).

### 2.3. Item & Menu: `QuantumGuidebookItem` (`quantum_guidebook`) & `QuantumGuidebookMenu`
- **Registry ID:** `quantumindustries:quantum_guidebook` (max stack size 1).
- **Menu Synchronization (`QuantumGuidebookMenu`):**
  - Registered in `ModMenuTypes.QUANTUM_GUIDEBOOK_MENU`.
  - Uses `ContainerData` of size `ResearchCatalog.STUDIES.size()` where slot `i` is `1` if `WorldResearchData.get(serverLevel).knows(player, i)` is true, and `0` otherwise.
  - Because it uses NeoForge's standard `MenuType` + `ContainerData` pipeline (identical to `ResearchTableMenu`), opening the book with `player.openMenu(...)` automatically syncs the player's exact research completion state from server to client with zero custom packet boilerplate!
  - `stillValid(Player)` always returns `true` so the book can be read anywhere.

### 2.4. Client UI & Catalog: `GuidebookCatalog` & `QuantumGuidebookScreen`
- **`GuidebookCatalog` (`br.com.quantumindustries.research.GuidebookCatalog`):**
  - Defines structured entries (`Entry(String id, String studyKey, int era, String title, String subtitle, Supplier<ItemStack> icon, List<String> paragraphs, String operationSummary)`).
  - **Entry 0 (`intro_vault`, `studyKey = null`, `era = 0`):** Always unlocked. Explains the crashed Vault, the mystery of the `????` encrypted chapters, and step-by-step instructions to craft the ** Caderno/Mesa de Pesquisa** and **Analisador de Materiais** to start unlocking the book.
  - **All Machine & System Entries (covering Era I and Era II studies):**
    1. `material_observation` — **Analisador de Materiais & Bancada de Ensaios Empíricos** (Inspeção, Impacto Mecânico, Aquecimento e Reação Ácida).
    2. `manual_forming` / `metal_sheets` — **Bancada de Conformação & Martelo de Forja** (Como bater chapas metálicas na bancada).
    3. `alloy_metallurgy` — **Forno de Ligas a Fole (`Bellows Alloy Kiln`)** (Como combinar cobre + estanho/zinco com carvão para fundir Bronze e Latão).
    4. `mechanical_transmission` — **Eixos Mecânicos, Serraria Mecânica & Moinho Manual** (Como transmitir rotação por eixos e processar madeira/grãos).
    5. `hydraulic_power` — **Roda-d'Água Primitiva (`Primitive Water Wheel`)** (Como posicionar água corrente ao redor da roda para gerar torque contínuo).
    6. `mineral_beneficiation` — **Britador de Mandíbulas (`Jaw Crusher`)** (Operação manual ou elétrica para dobrar minérios em Pós Minerais 2x).
    7. `copper_wire` / `electrical_insulation` — **Cabos de Cobre & Condutos Isolados** (Como posicionar cabos no chão/paredes e conectar geradores a acumuladores e máquinas).
    8. `manual_dynamo_design` — **Dínamo Manual (`Manual Dynamo`)** (Conversão de energia mecânica/manivela em Forge Energy `FE`).
    9. `galvanic_storage` — **Pilha Voltaica & Acumulador Chumbo-Ácido (`Voltaic Pile` / `Lead-Acid Accumulator`)** (Armazenamento de FE, monitoramento de carga e uso de eletrólito).
    10. `load_protection` / `piezoelectricity` — **Relé de Carga & Transdutor Piezoelétrico** (Automação de corte de bateria por Redstone e geração por impacto).
    11. `thermal_annealing` / `wire_drawing` — **Forja de Recozimento a Fole & Trefiladora de Fios** (Aquecimento de lingotes e trefilação eficiente de 4 fios por chapa).
    12. `electromechanical_forming` — **Prensa Eletromecânica (`Electromechanical Press`)** (Conformação industrial movida a FE).
    13. `electric_alloy_smelting` — **Fundidora de Ligas Elétrica (`Electric Alloy Smelter`)** (Fusão por indução a FE para Invar, Electrum, Constantan e Aço).
    14. `fluid_hydrostatics` / `peristaltic_mechanics` — **Tubos de Fluido, Bomba Peristáltica & Cuba de Decantação** (Transporte hidráulico, bombeamento a 50 mB/t e decantação).
    15. `pneumatic_transport` — **Tubos Pneumáticos, Ejetor a Fole & Classificador Gravitacional** (Logística de itens por pulso de ar e filtragem).
    16. `chemical_leaching` — **Lixiviação Ácida & Fertilizante Fosfatado** (Síntese de HCl diluído na Cuba de Decantação e uso do Superfosfato agrícola).
- **`QuantumGuidebookScreen` (`br.com.quantumindustries.client.QuantumGuidebookScreen`):**
  - Uses `ResearchTheme` (`DARK`, `PANEL`, `LINE`, `CYAN`, `GOLD`, `GREEN`, `TEXT`, `MUTED`) for visual consistency with the mod's UI.
  - **Left Sidebar:** Era filter tabs (`Intro`, `Era I`, `Era II`) + scrollable chapter list.
    - Unlocked chapters show their machine icon and real title in `TEXT` / `CYAN`.
    - Locked chapters (`studyKey != null && !menu.knows(studyIndex)`) show a lock icon and `????` in `MUTED`.
  - **Right Reading Pane:**
    - When **Unlocked**: Shows the chapter header, machine icon, study badge (`Conhecimento Desbloqueado`), multi-paragraph explanation of how the machine works, and a highlighted `OPERAÇÃO PRÁTICA` box at the bottom.
    - When **Locked (`????`)**: Shows `CAPÍTULO CRIPTOGRAFADO — ????`, obfuscated/scrambled lines (`???? ???? ???????? ????`), and a clear guidance box: *"Registros indecifráveis. Conclua a pesquisa [???? / ou nome da pesquisa se os pré-requisitos já foram revelados] na Mesa de Pesquisa para decodificar o funcionamento desta máquina."*

---

## 3. Testing & Verification Plan
- Unit tests in `tests/test_era2_features.py` (or a new `tests/test_vault_and_guidebook.py`) verifying:
  - Registration of `quantum_vault` block/item, `quantum_guidebook` item, and `quantum_guidebook_menu` menu type.
  - Valid JSON blockstate, block/item models, loot table, and `pt_br.json` / `en_us.json` translations.
  - `GuidebookCatalog` integrity: every `studyKey` referenced in `GuidebookCatalog` exists in `ResearchCatalog.STUDIES`, and no crafting recipe grids are exposed in the guidebook.
  - Gradle compilation (`./gradlew classes verifyLegacyResearch --no-daemon`) and Python test suite (`python3 -m unittest discover -s tests`) passing 100%.
