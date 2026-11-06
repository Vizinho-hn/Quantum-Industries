---
name: quantum-neoforge
description: Develop and verify Quantum Industries research, crafting, assay and machine features for its pinned Minecraft and NeoForge versions.
---

Read AGENTS.md and the relevant README/ROADMAP sections before editing.

- Registrations live in ModItems, core/ModBlocks and core/ModMenuTypes.
- Research definitions live in research/ResearchCatalog. WorldResearchData authorizes learning. Keep saved research identifiers stable.
- AssayCatalog defines empirical material/method identifiers. AssayProfile stores individual discoveries. Preserve historical keys during migrations.
- FoundationRecipe defines protected crafts and notebook previews. ModRecipeBook synchronizes vanilla recipes per player.
- Check evidence and research on the server. Reports use the same learning rules; admin commands may explicitly bypass them.
- Preserve existing completed research when introducing new requirements.
- Read the pinned API signatures from Gradle artifacts before changing Minecraft/NeoForge calls.
- Run focused tests, then the contract suite and relevant isolated-server harness. See tests/*.init.gradle and docs/MANUAL_DYNAMO.md. Keep previews and saves outside the normal JAR.
- Update README.md and ROADMAP.md with delivered behavior and remaining work.
