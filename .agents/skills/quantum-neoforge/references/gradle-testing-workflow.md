# Gradle, Verification Ladder & Live Minecraft Workflow

## Linux Environment Variables

Always ensure Java 25 and HD cache path before invoking Gradle:
```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export GRADLE_USER_HOME=/mnt/dados/cache/quantum-gradle
```
Limit parallel workers (`--max-workers=2`) to avoid starving desktop responsiveness.

## Proportional Verification Ladder (Cheapest → Most Expensive)

Run only up to the level justified by the change:

1. **Fast Python contract & resource suites (< 2s, no JVM):**
   ```bash
   python3 -m unittest discover -s tests -p 'test_*.py' -v
   ```
   (Covers `test_research_catalog.py`, `test_assay_resources.py`, `test_mechanical_resources.py`, `test_foundation_contract.py`, `test_grain_mill_contract.py`, `test_logistics_contracts.py`, `test_era2_features.py`, `test_independent_era_paths.py`, `test_placement_and_item.py`, `test_vault_and_guidebook.py`.)
2. **Java research & mechanical contract checks (CI):**
   ```bash
   ./gradlew --no-daemon --max-workers=2 verifyLegacyResearch verifyMechanicalFoundation
   ```
3. **Full compilation & check (CI):**
   ```bash
   ./gradlew --no-daemon --max-workers=2 build
   ```
   Do not re-run `build` repeatedly if no code/resource changed.
4. **Isolated dedicated-server integration suites (disposable worlds under `build/`, see `tests/*.init.gradle` and `docs/MANUAL_DYNAMO.md`):**
   - Assay bench: `./tools/verify-assay.sh` (`build/assay-verification/world`, port `25576`, asserts `PASS:` in `result.txt`)
   - Mechanical / restart persistence: `bash tools/verify-mechanical.sh` (`build/mechanical-verification/runs/...`, port `25578`, runs `write` then `read` server restarts)
5. **Visual GUI/world preview (Linux Xvfb, expensive — only for GUI/rendering changes):**
   - `./tools/preview-assay.sh` (requires `verify-assay.sh` world; produces 9 screenshots under `build/assay-preview/screenshots/`).

## Live In-Game Inspection (`minecraft-live` MCP & Autonomous QA Harness)

- Dev bridge JAR (`minecraft-mcp-26.1.2-neoforge-v0.4.28.jar`) is in `run/mods/` (gitignored; isolated from `verify-assay.sh` and `verify-mechanical.sh` because their init scripts override `gameDirectory`). Keep previews and test saves outside the normal JAR.
- `build.gradle` (`runs.client`) and `./Jogar.sh` pass `-Dmcp.remote_control=true` and `-Dmcp.background_input=true` (`MC_MCP_REMOTE_CONTROL=true`, `MC_MCP_BACKGROUND_INPUT=true`) so `enter_control_mode`, `screenshot`, `get_screen_buttons`, `enumerate_widgets`, `click_button_index`, `click`, and `set_view_angle` on port `19132` work without manual pause-menu "MCP Take Over".
- **Known `minecraft-mcp-26.1.2-neoforge-v0.4.28.jar` MC 26.1.2 caveats:** `execute_command`, `open_chat`, and `press_key` fail via reflection (`performCommand` vs `performPrefixedCommand`), and `get_player_info` breaks JSON formatting under `pt_BR` locale commas (`String.format("%.1f")`).
- **Autonomous single-command QA harness (`tests/qa-preview.init.gradle`):** launches `runClient` into isolated world `build/qa-preview/saves/MachinesQA` (`--quickPlaySingleplayer MachinesQA`), executes `MachinesQaSetup.java` + `MachinesQaClient.java`, captures native screenshots in `build/qa-preview/screenshots/`, writes `build/qa-preview/qa-report.txt`, and exits cleanly:
  ```bash
  ./gradlew --no-daemon --max-workers=2 --init-script tests/qa-preview.init.gradle runClient --console=plain > /tmp/quantum-qa-preview.log 2>&1
  ```
- **Blockbench MCP (`http://127.0.0.1:8787`):** if Blockbench is closed, launch in background with `DISPLAY=:0 /mnt/dados/apps/blockbench/squashfs-root/AppRun` and wait ~4s before calling `get_status`, `new_project`, `import_texture`, `add_cubes`, `check_model`, `screenshot_views`, `save_project`, and `export_model`.
- Use `minecraft-live` tools only on local dev worlds — **never** on public servers or production saves without explicit permission.

## Pre-PR Review Workflow

1. Review `git status --short` and full `git diff` (check client/server leaks, `en_us`/`pt_br` parity, `README.md`/`ROADMAP.md` accuracy).
2. Run relevant tests after the final edit and report exact commands + results.
3. Wait for explicit user approval before `git commit`, `git push`, or opening a PR.
