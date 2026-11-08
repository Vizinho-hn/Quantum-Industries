# Shared Graphify Context Design

## Intent

Let coding agents working on Quantum Industries consult the same generated architecture map, reducing repeated repository exploration while keeping source code authoritative.

## Approaches

- **Shared graph and guidance (chosen):** Track `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md`; add concise `AGENTS.md` instructions for querying the graph and validating conclusions against source. This gives agents a portable common reference without depending on machine-specific agent configuration.
- **Instructions only:** Ask each agent to build a local graph. This avoids generated files in Git, but does not give agents a common starting point.
- **Per-agent Graphify MCP setup:** Configure local agent integrations. This is machine-specific, broader than needed, and does not itself provide a shared graph artifact.

## Goals and constraints

- Share the machine-readable graph and its audit report in the repository.
- Explain how to query the graph and require checking relevant source files before making changes.
- Preserve visible integrity caveats; do not present graph clusters or inferred links as authoritative architecture.
- Keep HTML, extraction caches, manifests, cost data, interpreter paths, and query-memory files local.
- Do not modify gameplay code, add dependencies, or configure global agent settings.
- Leave `README.md` and `ROADMAP.md` unchanged because this does not change mod behavior or project progress.

## Implementation decisions

- Store shared outputs at `graphify-out/graph.json` and `graphify-out/GRAPH_REPORT.md`.
- Add a short Graphify navigation section to `AGENTS.md`, with `graphify query` as the preferred entry point and source-code verification as a requirement.
- Add narrow ignore rules for local Graphify caches and runtime metadata without ignoring the two shared outputs.
- Check that the JSON parses, the report records known limitations, the query command can read the committed graph, and Git shows only intended files.

## Open implementation choice

The local graph was generated from `src`, excluding 126 images. The implementation should preserve that scope and clearly state it in the report and agent instructions.
