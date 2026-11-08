---
title: "ADR 0008: Portable project MCP tooling"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [8, 9]
---

# ADR 0008: Portable project MCP tooling

## Context

Claude, Codex, Gemini and VS Code currently see different MCP sets. Some project files contain
machine-absolute paths, some rely on user-global configuration, and Gemini lacks the Java, Maven,
Vuetify and Docker tools required for this stack.

## Decision

`ops/mcp/servers.yml` is the source for generated client configurations. Server packages and
images are version or digest pinned. Wrappers derive the repository root at runtime and tracked
configuration contains no personal path or credential.

Java navigation uses JavaLens; Maven dependency and model questions use the Maven servers;
Vuetify work uses the Vuetify server. Docker inspection is read-only and controlled launchers own
stack mutation. Playwright supports browser recette and the Nuxt server is optional while the dev
server is stopped.

The MCP doctor initializes each core server, lists tools, performs a harmless representative call
and verifies discovery by Claude, Codex and Gemini. A missing core tool or conflicting client scope
is a failed workstation check, not permission to substitute remembered API details.

## Consequences

The same checkout advertises the same tools to supported agents. Optional personal integrations
move to ignored local overlays and cannot make project validation pass accidentally.
