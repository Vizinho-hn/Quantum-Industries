# Shared Graphify Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the `src` architecture graph a shared, safely queryable reference for coding agents working on Quantum Industries.

**Architecture:** Track only `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md`. Add concise Graphify navigation guidance to `AGENTS.md` and ignore generated local state so caches, absolute interpreter paths, and query history do not enter Git.

**Tech Stack:** Markdown, Git ignore rules, Graphify CLI 0.9.98, JSON.

**Spec:** `docs/superpowers/specs/2026-11-07-shared-graphify-context-design.md`

## Global Constraints

- Keep the graph scoped to `src`; the 126 image assets were excluded from semantic extraction by user choice.
- Track `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md` only.
- Keep HTML, extraction caches, manifests, cost data, interpreter paths, and query-memory files local.
- Do not modify gameplay code, add dependencies, or configure global agent settings.
- Leave `README.md` and `ROADMAP.md` unchanged because this does not change mod behavior or project progress.
- Do not commit or publish changes without explicit permission.

## Review Focus

- The graph JSON must remain parseable and include its relationship list.
- The report must disclose the source scope, excluded images, incomplete token accounting, and graph-health warnings.
- Ignore rules must not hide either shared graph artifact.
- Ignore rules must cover generated root and nested `src/graphify-out` caches.
- Agent guidance must require source verification and explain how to query the checked-in graph.

---

### Task 1: Share and document the architecture graph

**Files:**
- Modify: `AGENTS.md`
- Modify: `.gitignore`
- Create: `graphify-out/graph.json` (copy the already-generated artifact)
- Create: `graphify-out/GRAPH_REPORT.md` (copy the already-generated report)

**Interfaces:**
- Consumes: `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md` generated for the `src` tree.
- Produces: a Graphify query entry point documented in `AGENTS.md`; exactly two tracked Graphify outputs.

- [ ] Confirm the generated graph/report exist, parse the JSON, and verify `nodes`, `links`, and the documented caveats.
- [ ] Add narrow `.gitignore` rules for root Graphify runtime files and nested `/src/graphify-out/` cache; ensure the two shared artifacts remain visible to Git.
- [ ] Add a short `AGENTS.md` section telling agents to use `graphify query --graph graphify-out/graph.json` for architecture navigation when available, treat the graph/report as navigation rather than authority, and verify conclusions in source.
- [ ] Re-run `graphify query --graph graphify-out/graph.json` for `ModBlocks` and confirm it returns the shared graph.
- [ ] Inspect `git status --short` and verify only `AGENTS.md`, `.gitignore`, the two shared artifacts, and the approved design/plan documents are visible; no cache, HTML, manifest, cost, interpreter, or query-memory files are visible.
- [ ] Run `git diff --check` and inspect the complete diff. Do not commit or push.
