---
name: quantum-neoforge
description: Develop and verify Quantum Industries research, crafting, assay, and machine features for Minecraft 26.1.2, NeoForge 26.1.2.125, Java 25, and Gradle 9.2.1. Shared project skill for Antigravity, Codex, and Hermes.
---

# Quantum Industries — Engineering Skill

Shared repository skill for `Quantum-Industries` (`mod_id = quantumindustries`, group `br.com.quantumindustries`).

**Do not duplicate generic skills:** use `minecraft-modding-framework` for generic NeoForge lifecycle rules, `blockbench-model-architect` for 3D modeling/Blockbench, `datapack-json-automator` for bulk JSON/datapack syntax, and `minecraft-code-intelligence` for Graphify usage.

## 1. Confirmed Stack & Authority

- **Pinned versions (never bump without explicit alignment):** Minecraft `26.1.2`, NeoForge `26.1.2.125`, ModDevGradle `2.0.148`, Java `25`, Gradle Wrapper `9.2.1`, GeckoLib `5.5.2`, JEI `29.53.0.115`, Jade `26.1.12+neoforge`.
- **Canonical docs:** Read `AGENTS.md` and `CONTRIBUTING.md` first, plus only the relevant sections of `README.md`, `ROADMAP.md`, and `docs/`.
- **Navigation without repo dumps:** Query `graphify query "<question>" --graph graphify-out/graph.json` or `graphify-out/GRAPH_REPORT.md` when available, then verify in source. `docs/ARCHITECTURE.md` includes conceptual modules not yet implemented.
- **API ground truth:** Never invent or backport signatures. Query `mcdev` MCP (`mc_search`, `mc_get_class`, `mc_get_method`, `mc_find_hierarchy`, `mc_find_refs`) for Minecraft `26.1.2` sources, and inspect existing `Quantum-Industries` / Gradle dependency sources for NeoForge APIs.

## 2. Core Repo Rules

1. Check `git status --short` first and preserve pre-existing changes.
2. Work on `feat/*`, `fix/*`, or `docs/*` branches; never push directly to `main`, force-push, commit, merge, or delete files without explicit approval.
3. Make the smallest correct change. Preserve IDs, registries, saves, per-player research isolation, and client/server separation.
4. Keep previews, saves, `run/`, `logs/`, `.gradle/`, and `build/` out of commits and packaged JARs.
5. Update `README.md` and `ROADMAP.md` in the same PR when delivered behavior or remaining work changes.

## 3. On-Demand References (Load Only What You Need)

| Reference | Scope |
| :--- | :--- |
| [neoforge-26-java25.md](references/neoforge-26-java25.md) | MC `26.1.2` / NeoForge `26.1.2.125` API deltas, Java 25 toolchain, `mcdev` MCP usage |
| [registries-blockentities-gui.md](references/registries-blockentities-gui.md) | `ModItems`, `core/ModBlocks`, `core/ModMenuTypes`, `ModRecipes`, `BlockEntity` sync, GUIs, networks |
| [research-and-multiplayer.md](references/research-and-multiplayer.md) | `ResearchCatalog`, `WorldResearchData`, `LegacyResearch`, `FoundationRecipe`, `ModRecipeBook`, `AssayCatalog`/`AssayProfile` |
| [assets-blockbench-json.md](references/assets-blockbench-json.md) | `design/*.bbmodel`, GeckoLib, blockstates, 1.21.4+ `items/*.json`, `en_us`/`pt_br` parity |
| [gradle-testing-workflow.md](references/gradle-testing-workflow.md) | Python contract tests, `verifyLegacyResearch`/`verifyMechanicalFoundation`, isolated server/preview harnesses, `minecraft-live` MCP |

## 4. Verification & Delivery

Run focused checks first, then the contract suite and relevant isolated-server harness (`tests/*.init.gradle`, `docs/MANUAL_DYNAMO.md`). Never claim a build, test, or visual check passed unless you ran it after your final edit.

Report: (1) what changed and why, (2) files modified, (3) exact checks run and results/limitations, (4) risks or pending approvals.
