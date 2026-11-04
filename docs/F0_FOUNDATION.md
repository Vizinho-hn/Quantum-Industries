# F0 — Foundation and research UI stabilization

Status: **review candidate**, not verified inside Minecraft yet.

## Scope
- One active research screen: `src/main/java/br/com/quantumindustries/client/ResearchTableScreen.java`.
- Replace both old interface implementations with an independent research-first UI.
- Keep the original slot layout and inventory menu contract for legacy saves.
- Display the only real server-authoritative discovery (`conductivity`).
- Future technology nodes remain anonymized dark shapes. New research states and tooltips belong to the F1/F5 implementation, not fake client-side flags.
- Prevent BlockEntity ticker from ticking incompatible types.
- Structural regression checks in `tests/test_foundation_contract.py`.

## Compatibility policy (critical)
F0 deliberately does **not** rename any registered block, item, menu or block-entity ID. In particular:
- Block `quantumindustries:scientific_analyzer`
- Block entity `quantumindustries:research_table`
- Menu `quantumindustries:research_table_menu`
- World SavedData ID `quantumindustries:research`
- Legacy SavedData codec field `conductivity` stays a boolean.
- The BlockEntity save keys `progress` and `slot_0` through `slot_2` remain unchanged.

The machine still performs its *legacy* copper analysis and wire output in F0, solely to keep save and gameplay contracts stable. Separating wire production into a different machine is a **subsequent migration**, not silently part of this PR.

## Tests
Automated CI:
1. Run Python stdlib structural regression checks.
2. Java 25 Gradle `build`.
3. Publish a JAR artifact only after both pass.

Manual acceptance required before merging:
1. Back up a pre-F0 world with a placed research bench, input items, output and partial progress.
2. Start a fresh development client and open the Research Bench; verify no duplicate screen exception.
3. Confirm only the new UI is used; select the Research Tree and Knowledge tabs.
4. Confirm slots, shift-click, mouse interaction and all player inventory slots are usable.
5. Save/reopen the pre-F0 world, inspect block, machine contents, research state and processing progress.
6. Test two clients using the same world and check server sync of conductivity.
7. Test at several GUI scales and at reduced window size.
8. Break the block and confirm no lost or duplicated inventory contents.
9. Start a dedicated server to ensure client-only classes aren't loaded by common code.

## Deliberately deferred
- Materials analysis and production move out of Research Bench.
- General research system, JSON definitions, completed/revealed nodes.
- Interactive tech tree navigation, tooltips, research selection / remote requests.
- Versioned SavedData migration to structured research profiles.
- Any new craft or capability unlock.

Do not merge solely because compilation passes: menu and existing save-game interaction need in-game validation.
