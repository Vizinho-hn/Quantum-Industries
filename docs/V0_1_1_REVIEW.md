# v0.1.1 — First research vertical slice (experimental)

This branch is a reviewable **unverified** implementation. Do not merge until the project compiles and can be tested in-game using Java 25 and NeoForge 26.1.2.

## Implemented

- One canonical `ModBlocks.BLOCKS` registration; removed duplicate deferred block register in `ModItems`.
- Copper wire item registered and translated; temporary **copper ingot** item texture (not final art).
- ResearchTableBlockEntity input/output validation; full-output guard; per-slot inventory serialization and progress saving.
- World-global `SavedData` flag `conductivity`, shared between players and dimensions.
- Before discovery: insert one raw copper or copper ingot and one test probe; after 200 server ticks, a `research_data` item is produced and conductivity is unlocked for the world.
- After discovery: the same analyzer processing inputs produce **2 copper wires**, using one copper and probe durability. This is an interim protected manufacturing path.
- Output slot blocks insertions, and shift-click input insertion never targets output.

## Important limitations / reviewer checklist

- **No vanilla crafting-table recipe for copper wire is shipped:** a normal crafting recipe can be crafted manually regardless of a recipe-book unlock, which would bypass research. Introduce a secure custom crafting recipe or server-side gating in a later PR.
- No research selection UI or custom Screen was added; the existing research menu wiring still needs in-game verification.
- Need to verify inventory serialization with 26.1.2 APIs and block-break item drops (possible drops/duplication); automated tests absent.
- Recipe flow differs from the proposed 3 copper + redstone + paper cost. This PR intentionally preserves the current copper + test-probe analyzer interaction as the first playable slice, pending a redesigned multi-input research UI.
- Check consistent block and item identifiers (`scientific_analyzer` vs `research_table`).
- **Run** `./gradlew compileJava processResources`, `./gradlew runClient`, then test world save/reload and two players in multiplayer.
- Do not treat this PR as production-ready without compiler and gameplay checks.

## Acceptance tests

1. Before analysis, copper wire cannot be fabricated at a vanilla crafting table.
2. Insert copper and the test probe. After processing, `research_data` appears; inventory does not consume items when output is full.
3. Remove output; run analyzer again. Copper wire should be produced.
4. Save and reload: unlocked conductivity and inventory content remain intact.
5. Confirm a second player can use the unlocked wire process in the same world.
6. Check the overworld and Nether access the same research flag.
