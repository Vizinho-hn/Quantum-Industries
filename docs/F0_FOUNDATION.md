# Quantum Industries — F0 + Research Starter (2-node vertical slice)

The Research Bench is now a **research-only terminal**, not a machine that accepts samples or makes items. All item processing was removed from its interface and tick logic.

## Gameplay

- The research UI contains only two real technologies: `Condutividade` and `Fio de Cobre`.
- `Condutividade` is initially available. Research it by clicking its node, then `PESQUISAR` with **3 copper ingots, 1 redstone dust and 1 paper** in the player's inventory.
- `Fio de Cobre` is fully concealed until Condutividade is completed. Afterward, unlock it for **2 copper ingots and 1 paper**.
- Both studies are permanent and world-shared.
- On a vanilla crafting table, place **3 copper ingots in one horizontal row → 6 copper wires**. The custom recipe's `matches()` verifies the world research state **on the server**, preventing manual bypass before both studies.
- The copper wire item's icon currently uses a placeholder vanilla copper texture. Art will be added separately.

## World/save compatibility

- Keep registry IDs `scientific_analyzer`, `research_table`, `research_table_menu`.
- Keep SavedData ID `research`; `conductivity` remains a required boolean as in prior worlds.
- Add `copper_wire` as an **optional** boolean defaulting to false, so worlds with only `conductivity` still deserialize.
- Keep legacy BlockEntity NBT keys `progress` and `slot_0`-`slot_2`, so loaded worlds preserve their old machine items. The terminal no longer processes them.
- If the block is broken, legacy items are dropped via `preRemoveSideEffects`. Keep a backup of worlds before testing any upgrade.

## Technical architecture

- `WorldResearchData` owns server-side world-shared research flags.
- `ResearchTableMenu#clickMenuButton` verifies reach, prerequisites, available inventory contents and double spends on the **server**.
- `ResearchTableScreen` only renders synchronized flags and requests via the vanilla container-button packet; it never alters research data.
- `CopperWireRecipe` is a custom special crafting recipe (registered serializer), not an unlocked vanilla shaped recipe. The server validates research before it matches.

## Automated gates

Run `python3 -m unittest discover -s tests -p 'test_*.py' -v` and `./gradlew build`. GitHub Actions executes both.

## Manual acceptance before gameplay release

1. Open Minecraft, right-click Research Bench, confirm no sample/probe/output slots.
2. Check the two horizontal nodes and tabs at multiple GUI scales.
3. Place 3 copper across the crafting grid **before** unlock: no wire result.
4. Research Condutividade; confirm no way to research it a second time.
5. Confirm Fio de Cobre node becomes revealed, and is not completed yet.
6. Research Fio de Cobre; craft 6 wire from 3 copper in crafting table; check normal and shift-click results.
7. Reload world; both completed studies persist.
8. Multiplayer: second player sees state; crafting is impossible before research and possible afterward.
9. Upgrade a saved 0.1.1 world containing conductivity=true and machine inventory; confirm conductivity remains done and break block to recover old inputs.
10. Dedicated server: start without client-only registration crash.

## Limitations and next phase

The two starter researches currently use inventory item costs to provide a playable vertical slice, not the full evidence-based Analyzer workflow. The upcoming ResearchManager and KnowledgeDatabase will use generic technological capabilities and scientific evidence rather than one research per metal. No background research points or automation yet.
