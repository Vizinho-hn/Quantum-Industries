# Quantum Kinetic System — implementation and migration

**Status:** PR 1 integrates QKS Core, Network Solver, Chunk Sync and Native Renderer. Component dynamics drive the existing mechanical machines in game, verified with headless mechanics checks, dedicated-server regression suites (`verify-mechanical.sh`, `verify-hydraulic-dynamo.sh`), and client visual previews (`mechanical-preview`, `qa-preview`).

**Scope:** shafts, the existing one-block water wheel, sawmills and dynamos use server-authoritative QKS dynamics and a native renderer without GeckoLib API consumers. Cogwheels, flywheel, pulleys and 3×3/5×5 wheels belong to PR 2. Inspection Goggles kinetic telemetry, expanded optional Codex physics chapters and the full `verify-qks.sh` belong to PR 3. The globally declared GeckoLib dependency is outside this delivery.

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

1. **Server topology:** bounded traversal validates ratios and inconsistent cycles without loading chunks. Existing mechanical ports supply links; ratio support does not mean gear or pulley blocks ship in PR 1.
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
- Keep the existing one-block wheel and original assets. Larger wheels need dedicated geometry, collision, water sampling and inertia work in PR 2.
- Minecraft flow is a gameplay input, not an exact scientific measurement of discharge or hydraulic head. Preserve the established axial torque sign convention.
- Optional physics reading must not add research or crafting gates. The existing introductory chapter remains available; expanded chapters are PR 3.

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

### Stage D — Transmissions and larger wheels (PR 2 upcoming)

- [ ] Cogwheels with tooth ratios, reversal and speed/torque tradeoffs.
- [ ] Flywheel and pulley/belt blocks with appropriate losses and inertia.
- [ ] 3×3 and 5×5 water wheels, multiblock geometry/collision and water sampling.
- [ ] In-game acceptance of the new transmissions and cycle/energy safety under their actual block topology.
- [ ] Comparable CPU/MSPT, GPU and network measurements; no performance gain is claimed by PR 1.

### Stage E — Telemetry and optional physics (PR 3 upcoming)

- [ ] Inspection Goggles kinetic capacity/load, direction, connections and stop diagnostics.
- [ ] RPM, acceleration, power direction and vector overlays derived from simulated values.
- [ ] Expanded optional Codex chapters on circular motion, torque, gears and practical operation, without crafting gates.
- [ ] Full `verify-qks.sh` harness.

## 6. Verification and remaining coverage

PR 1 verification passes across the Python contract suite (`unittest`), Gradle build & headless checks (`build`, `verifyLegacyResearch`, `verifyMechanicalFoundation` including `QksGeometrySelfCheck` and `1`–`256` node microbenchmarks), dedicated-server suites (`tools/verify-mechanical.sh`, `tools/verify-hydraulic-dynamo.sh`), and client visual previews (`tests/mechanical-preview.init.gradle`, `tests/qa-preview.init.gradle`).

Future acceptance across PR 2 and PR 3 will cover cogwheels, flywheels, pulleys/belts, 3×3/5×5 water wheels, extended multiplayer inspection telemetry (`Inspection Goggles`), and consolidated dedicated-server stress testing (`tools/verify-qks.sh`).
