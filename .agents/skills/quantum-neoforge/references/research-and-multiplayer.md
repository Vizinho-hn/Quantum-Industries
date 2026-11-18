# Scientific Research, Assay Bench & Multiplayer Persistence

## Core Architecture & Non-Negotiable Invariants

1. **Per-player individual knowledge:** Progress is keyed by player UUID in `WorldResearchData` (`completed_research` map) and `AssayProfile` (`player.getPersistentData()` under `"quantum_assay_v1"`). Sharing a machine or bench must **never** leak or merge discoveries between players.
2. **Server-side validation:**
   - Every study unlock goes through `WorldResearchData.canLearn` / `learn` on `ServerLevel` before deducting items or consuming reports.
   - Hiding a recipe in the GUI is never enough. Protected recipes use `FoundationRecipe` + `ResearchCraftingAccess` + `CraftingMenuMixin` (`ThreadLocal<Player>` cleared in `finally`). Automation without a player (`crafter(level) == null`) cannot craft protected recipes. `ModRecipeBook` synchronizes vanilla recipes per player.
3. **Stable IDs & Legacy Compatibility:**
   - Study IDs live in `ResearchCatalog.STUDIES` (`ResearchIds.of(key)`). Startup validation rejects duplicates, invalid prerequisites, and dependency cycles (verified by `tests/test_research_catalog.py` and `LegacyResearchCheck`).
   - `LegacyResearch` order is frozen to decode old bitmasks and legacy reports. Modern `completed_research` entries (even empty after `/quantum:clear_conhecimento`) take precedence over legacy bitmasks. Legacy global progress is inherited only by an identifiable singleplayer host, never on dedicated servers.
   - New scientific evidence requirements must preserve already-completed studies (`WorldResearchData` keeps existing entries valid).
4. **Eras are thematic groupings, not global gates:** Parallel branches (hydraulics, pneumatics, thermal annealing, wire drawing) only require their own declared `prerequisites` and `evidence` in `ResearchCatalog`.

## Empirical Assay Bench (`empirical_assay_bench`) Invariants

- **Scope:** 13 materials × 4 methods (`INSPECTION`, `HEATING`, `REACTION`, `MECHANICAL`) = 52 discoveries (`AssayCatalog`). Raw copper is accepted as a copper sample variant.
- **Tools (reusable, never consumed):** `magnifying_glass` (inspection), `manual_bellows` or legacy flame/flint-and-steel (heating), `assay_flask` (reaction), integrated mechanism (mechanical — old `brass_weights` are legacy-only, not required).
- **Sample reservation & persistence:**
  - Starting the first valid 200-tick (10s) assay reserves 1 sample in `AssayProfile` (`reserved_material`); the HUD indicator is not a second removable item and does not drop when breaking the block.
  - Closing the GUI, logging out, or switching benches preserves the player's open cycle and completed observations; interrupting a running test cancels only the timer.
  - Tools stay in the bench's 4 physical slots, persist with the block, and drop on block break.
  - Legacy `galvanic` history is preserved in `AssayProfile` / `EmpiricalAssayBenchBlockEntity` (`assay_version < 2`) and must **never** be reinterpreted as `reaction` or `inspection`.
- **Sharing knowledge:** `EmpiricalFieldNotesItem` / `ResearchReportItem` share observations or reports explicitly; reading/assimilating still respects server validation and never bypasses missing requirements.
- **Admin commands (`ResearchCommands`):** `/quantum:desbloquear_pesquisas` and `/quantum:clear_conhecimento` (clears studies while keeping experimental profile).
