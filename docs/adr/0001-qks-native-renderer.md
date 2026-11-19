# ADR 0001 — Native mechanical renderer and client snapshot delivery

Status: implemented in PR 1; client preview and server integration QA completed.

Context: the four mechanical models must preserve geometry, UVs and hierarchy while the server supplies kinetic phase. A packet can arrive before its chunk entity.

Decision: `QksBlockEntityRenderer.register` registers all four entities and initializes the client sink (`src/client/java/br/com/quantumindustries/client/kinetic/QksBlockEntityRenderer.java:51`). Bone submission is recursive and preserves children of a hidden base (`QksBlockEntityRenderer.java:136`). `ClientKineticTracker` queues at most 1024 pending snapshots, expires them after 100 ticks and clears the queue when the world instance changes (`src/client/java/br/com/quantumindustries/client/kinetic/ClientKineticTracker.java:20`). The renderer uses entity phase extrapolation rather than introducing another phase state.

Rejected alternatives: retaining the previous renderers would retain GeckoLib API coupling; discarding packets without an entity would lose the initial snapshot; adding phase correction without measuring discontinuities would duplicate state without evidence.

Consequences: mechanical classes have no GeckoLib API dependency. The legacy `geckolib/models/block/` resource path remains compatible; removing the globally declared dependency is outside PR 1. Existing models and textures are preserved. Future changes must retain geometry, phase and sawmill/dynamo production regressions.

Validation: verified with `QksGeometrySelfCheck` and `MechanicalFoundationCheck`, dedicated-server regression suites (`tools/verify-mechanical.sh` and `tools/verify-hydraulic-dynamo.sh`), and in-game client screenshot captures (`tests/mechanical-preview.init.gradle` and `tests/qa-preview.init.gradle`). PR 2 adds transmissions and larger wheels; PR 3 adds kinetic telemetry, expanded Codex physics chapters and the full `verify-qks.sh` harness.
