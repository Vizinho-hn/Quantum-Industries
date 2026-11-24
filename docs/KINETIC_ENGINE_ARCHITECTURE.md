# Quantum Kinetic System — implementation and migration

**Status:** Stages A–E are implemented across PR 1 (core, network, chunk sync and native renderer), PR 2 (transmissions and larger wheels), and PR 3 (inspection telemetry, optional physics chapters and dedicated-server verification harness). PR 3 dedicated-server acceptance passed end-to-end with `bash tools/verify-qks.sh` (write/read phases and class isolation in both JVMs).

**Scope:** server-authoritative dynamics for shafts, sawmills, dynamos, cogwheels, flywheel, pulleys/belts and 1×1/3×3/5×5 water wheels. Existing geometry uses the native renderer; the globally declared GeckoLib dependency is outside this delivery. PR 3 runtime acceptance is verified by `bash tools/verify-qks.sh`; the client preview verifies the normal goggles HUD, Shift diagnostics/topology and optional Codex pagination.

## 1. Implemented PR 1 components

Paths below are relative to `src/`; line references identify declarations or entry points inspected for this update.

| Component | Responsibility | Source |
| --- | --- | --- |
| `KineticDynamics` | Game-independent integration, static friction, reflected inertia/torques and momentum | `main/java/br/com/quantumindustries/mechanical/KineticDynamics.java:8` |
| `KineticNetworkManager` | Bounded topology evaluation, one reference rotor per component, stop classification and chunk tracking sync | `main/java/br/com/quantumindustries/mechanical/KineticNetworkManager.java:22` |
| `KineticSnapshot` | Authoritative phase, velocity, acceleration, ratings and stop state; moving direction follows velocity | `main/java/br/com/quantumindustries/mechanical/KineticSnapshot.java:3` |
| `KineticLink` | Validated signed transmission ratio and efficiency; extension point for PR 2 | `main/java/br/com/quantumindustries/mechanical/KineticLink.java:6` |
| `KineticSyncPayload` | Validated client payload with position, reference time, motion, ratio and operating state | `main/java/br/com/quantumindustries/mechanical/KineticSyncPayload.java:9` |
| `KineticNetworking` | Registers clientbound payload handling and delivers through a client sink | `main/java/br/com/quantumindustries/mechanical/KineticNetworking.java:7` |
| `StopReason` | No source, overload, opposing sources, jam and network limit, distinct from residual motion | `main/java/br/com/quantumindustries/mechanical/StopReason.java:3` |
| `ClientKineticTracker` | Bounded pending delivery across the chunk/entity gap; expiry and world-instance cleanup | `client/java/br/com/quantumindustries/client/kinetic/ClientKineticTracker.java:20` |
| `QksBlockEntityRenderer` | Native rendering of all four machines; hierarchical static and moving bones | `client/java/br/com/quantumindustries/client/kinetic/QksBlockEntityRenderer.java:30` |
| `QksCompiledModel` | Immutable compiled geometry with separate bone transforms | `client/java/br/com/quantumindustries/client/kinetic/QksCompiledModel.java:16` |
| `QksGeoModelLoader` | Compiles existing Bedrock geometry and per-face UVs without an animation library | `client/java/br/com/quantumindustries/client/kinetic/QksGeoModelLoader.java:17` |
| `QksModelCache` | Cached meshes and resource reload invalidation | `client/java/br/com/quantumindustries/client/kinetic/QksModelCache.java:18` |
| `QksRenderState` | Detached state used during render submission | `client/java/br/com/quantumindustries/client/kinetic/QksRenderState.java:8` |
| `QksGeometrySelfCheck` | Headless geometry and emission checks | `client/java/br/com/quantumindustries/client/kinetic/QksGeometrySelfCheck.java:21` |

## 2. Runtime boundaries

1. **Server topology:** bounded traversal validates ratios and inconsistent cycles without loading chunks. Existing mechanical ports supply links; PR 2 supplies physical gear meshes and reciprocal pulley links.
2. **Server dynamics:** one reference rotor per component advances angle, signed velocity and acceleration with reflected inertia, source torque, load and losses. Capacity/load remain gameplay UM. Stop classification gates production separately from coasting.
3. **Synchronization:** snapshots are sent on relevant state changes, periodically while moving and on chunk watch. The wire payload carries a subset of the server snapshot; network ID, topology version and full ratings are not transmitted. The client sink is initialized during renderer registration. Pending packets are bounded, expire and are cleared on world changes.
4. **Native rendering:** existing meshes, UVs, hierarchy and pivots are retained. Stationary bearings remain separate from rotors; hiding base geometry does not hide child rotors. Entities extrapolate the authoritative phase/time pair and clamp braking before an uncommanded reversal. See [ADR 0001](adr/0001-qks-native-renderer.md).

## 3. Dynamics contract

```text
netTorque = sum(signed source torques) - opposing load - drag * omega
alpha = netTorque / equivalentInertia
newOmega = clamp(oldOmega + alpha * dt, -maxSpeed, +maxSpeed)
newTheta = wrap(oldTheta + (oldOmega + newOmega) / 2 * dt)
dt = 0.05 seconds at 20 TPS
```

`KineticDynamics.advance` implements equivalent-rotor integration (`main/java/br/com/quantumindustries/mechanical/KineticDynamics.java:32`); `advanceDetailed` adds friction and diagnostics (`KineticDynamics.java:64`). Insufficient drive cannot start against load. Removing water decelerates either rotation sign toward rest without granting production. While moving, snapshot direction follows signed local velocity; near rest it falls back to signed drive.

Ideal gears obey `omega2 / omega1 = -teeth1 / teeth2`; torque and inertia must be reflected to the same reference shaft, with efficiency losses. Power is `torque * omega`; stored rotational energy is `inertia * omega² / 2` and must come from prior work. These gameplay scales are not calibrated N·m or kg·m². Do not duplicate capacity, energy or reflected inertia.

## 4. Compatibility and progression

- Preserve UM, IDs, recipes, saves, per-player research, chunk boundaries and dynamo FE accounting.
- Retain the legacy topology API during migration; the QKS manager owns runtime motion. `MechanicalNetwork.invalidate` also invalidates QKS topology (`main/java/br/com/quantumindustries/mechanical/MechanicalNetwork.java:86`).
- A rotating machine is not necessarily powered. Torque/friction helpers describe dynamics; callers must apply stop/overload checks before delivering work. The sawmill checks both power and speed (`main/java/br/com/quantumindustries/block/entity/MechanicalSawmillBlockEntity.java:93`).
- Keep the existing one-block wheel and original assets. PR 2 adds larger wheels with dedicated geometry, collision, water sampling and inertia.
- Minecraft flow is a gameplay input, not an exact scientific measurement of discharge or hydraulic head. Preserve the established axial torque sign convention.
- Optional physics reading must not add research or crafting gates. PR 3 expands the optional chapters without adding research or crafting prerequisites.

## 5. Delivery stages

### Stage A — Testable core (PR 1 complete)

- [x] Game-independent integration with deterministic checks, friction, reflected quantities and momentum.
- [x] Existing optional introductory rotational physics chapter, preserving research gates.
- [x] Document contracts, compatibility and delivery boundaries.

### Stage B — Real network dynamics and chunk sync (PR 1 complete)

- [x] Component state for angle, velocity, acceleration and equivalent inertia; source/load/loss evaluation once per tick.
- [x] Lightweight motion snapshots, bounded synchronization and initial delivery on chunk watch.
- [x] Machine power gating and completed server regression suites for sawmill/dynamo production and persistence.
- [x] Water-driven acceleration, coasting after water removal and reversal through zero.
- [x] Bounded topology, opposing-source classification and inconsistent-ratio cycle detection.

### Stage C — Native renderer (PR 1 complete)

- [x] Native renderer for shaft, one-block water wheel, sawmill and dynamo, with existing geometry and textures.
- [x] No GeckoLib API usage in the mechanical subsystem; retained resource directory names do not imply API coupling.
- [x] Compiled meshes, resource reload handling, detached render state and hierarchical bone submission.
- [x] Headless geometry/rotor-sign checks and completed client preview QA for PR 1.

### Stage D — Transmissions and larger wheels (PR 2 complete)

- [x] `small_cogwheel` (Z=8) and `large_cogwheel` (Z=16) with tooth ratios, reversal and speed/torque tradeoffs.
- [x] `iron_flywheel` and `mechanical_pulley` + `drive_belt` with appropriate losses and inertia.
- [x] `medium_water_wheel` (3×3) and `large_water_wheel` (5×5), multiblock geometry/collision and water sampling.

### Stage E — Telemetry and optional physics (PR 3 complete)

- [x] Inspection Goggles capacity/load, direction, connections and QKS stop diagnostics.
- [x] RPM, angular velocity/acceleration, torque, inertia, power and ratio telemetry derived from server snapshots.
- [x] Expanded optional Codex chapters on circular motion, torque, gears and practical operation, without crafting gates.
- [x] Dedicated-server `bash tools/verify-qks.sh` passed write/read restart phases and class isolation in both JVMs.

## 6. Verification and remaining coverage

PR 1 verification passes across the Python contract suite (`unittest`), Gradle build & headless checks (`build`, `verifyLegacyResearch`, `verifyMechanicalFoundation` including `QksGeometrySelfCheck` and `1`–`256` node microbenchmarks), dedicated-server suites (`tools/verify-mechanical.sh`, `tools/verify-hydraulic-dynamo.sh`), and client visual previews (`tests/mechanical-preview.init.gradle`, `tests/qa-preview.init.gradle`).

PR 3 dedicated-server acceptance passed end-to-end on both JVMs: the write phase and the read restart persistence phase completed successfully (`read assertions=30`: full server restart, rotor phase/omega, belts, FE and resumed motion). The audit reported `PASS: dedicated-server class isolation (both JVMs)`. Verified command:

```bash
bash tools/verify-qks.sh
```

The script launches two dedicated-server JVMs with one isolated run directory under `build/qks-verification/runs/`. Each phase must emit a fresh `PASS` result and exit successfully; the second loads the first phase's saved chunks. Evidence includes phase logs, `restart-ready.txt` (persisted angle/omega and FE expectations), `complete.txt`, and JVM class-loading logs. Any client class loaded under `net.minecraft.client` or `br.com.quantumindustries.client` fails the final audit. A 600-second timeout bounds each phase.

The harness runs on real dedicated `ServerLevel` blocks using a `FakePlayer` with authorized research to insert oak logs and configure the dynamo through production APIs, without mocked worlds or client code. Controlled real fluid block states are refreshed before ticks to isolate signed flow from channel erosion; this tests hydraulic sampling and runtime dynamics, not natural river generation. Coverage includes both axle orientations, exact torque signs, balanced sources, reversal, dry/coasting power gates, one/three/four sawmill loads, dynamo buffer/freewheel, both gear ratios and mesh efficiency, flywheel inertia, both belt crossings/refund, larger-wheel partial wetting, topology split/merge and restart continuity. Three adjacent gears cannot form a triangular nearest-neighbor mesh on the voxel grid; the contradictory fixture closes their two meshes with a crossed return belt.

The headless `verifyMechanicalFoundation` microbenchmarks above are separate from the live dedicated-server `verify-qks.sh` benchmarks. Live `QKS runtime benchmark` mean rebuild+solve timings (10 samples per size, after warmup) were: 1 node: **910.38 µs**; 32 nodes: **962.78 µs**; 128 nodes: **1136.74 µs**; 256 nodes: **1360.58 µs** (run `qks-jignDTos`). These measure invalidated manager rebuild/solve calls over real connected shaft networks, including other active fixtures and packet dispatch overhead; they are not isolated MSPT, GPU measurements or a performance improvement claim. Remaining coverage includes natural-channel and multiplayer visual QA, and representative CPU/GPU/network measurements.

The client fixture (`tests/qks-preview.init.gradle`) equips real Inspection Goggles, aims at a working cogwheel, and requires a nonzero server-authored RPM snapshot before capturing the normal HUD and Shift diagnostics/topology. It also opens the optional physics reference with the existing Mechanical Transmission research and checks the next-page button. Eight decoded PNGs and `preview-result.txt` are written under `build/qks-preview/`; QA sources stay outside the production JAR. Captures were reviewed at 1280×800 with GUI scale 2, in English; other resolutions and Portuguese layout remain outside this visual check.

- [Normal goggles HUD](images/qks/goggles.png)
- [Shift diagnostics and network overlay](images/qks/goggles-diagnostics.png)
- [Optional Codex reference, page 2](images/qks/codex-page2.png)

## 7. Telemetry boundary and verification decision

The server inspection registry dispatches mechanical requests to `MechanicalInspectionProvider` (`src/main/java/br/com/quantumindustries/inspection/MechanicalInspectionProvider.java:22`), which uses `KineticNetworkManager` snapshots rather than client estimates. `InspectionSnapshot` (`src/main/java/br/com/quantumindustries/inspection/InspectionSnapshot.java:13`) preserves its original fourteen components, then appends angular velocity, acceleration, RPM, drive/load/net torque, equivalent/local inertia, mechanical power, effective ratio and stop reason. Its registry-aware stream codec (`InspectionSnapshot.java:94`, same directory) carries the same server-authored values. The goggles HUD and mechanical overlay consume these measurements; optional Códice physics chapters explain them without gating research.

**Decision (PR 3):** extend the established dedicated-server write/read harness pattern for QKS. A pure solver test cannot establish block topology, production gates, world saves or server class isolation; a visual preview cannot establish a fresh dedicated-server restart. Consequence: acceptance costs two server boots and requires JDK 25/Gradle cache access. Keep this test-only source set opt-in through `tests/qks.init.gradle`, outside the production JAR.

## 8. On-demand hydraulic contribution inspection

`HydraulicInspection` and its dedicated request/reply codec expose immutable samples retained by the existing wheel evaluation. Details are allocated only on inspection and remain separate from the basic `InspectionSnapshot` wire format. Effective contributions sum to local hydraulic drive after primitive saturation or rim normalization; network load, friction and equivalent inertia retain their existing QKS ownership. [Protocol, units, validation and six in-game captures](HYDRAULIC_INSPECTION.md).

### Compact gearbox extension (validation pending)

`MechanicalParticipant.kineticPortRatio` maps each mechanical face to its local generalized rotor, default +1 for legacy nodes. An axial link has signed ratio `ownPort / neighborPort`; `kineticPortEfficiency` multiplies both endpoint factors. The gearbox enables two perpendicular axes at once, with ratios +1 and -1 and 0.98 per port. The excluded axis is the persisted blockstate `AXIS`. Its own inertia and drag are added exactly once by the existing solver; capacity is shared. No changes to snapshot fields or world IDs.
