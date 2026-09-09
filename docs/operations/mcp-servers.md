---
title: "MCP servers"
normative: false
audience: PROJECT_SCOPED
---

# MCP servers

The machine contract is `ops/mcp/servers.yml`. It generates the Claude, Codex,
Gemini and VS Code project files through:

```bash
python3 scripts/generate/generate_mcp_configs.py
python3 scripts/generate/generate_mcp_configs.py --check
```

Core servers cover JavaLens navigation, Maven dependencies and project models,
Vuetify component contracts and read-only Docker inspection. Playwright, Context7,
Plausible and the live Nuxt endpoint are optional. Packages and container images are
pinned, and wrappers derive the checkout path rather than embedding a workstation path.

Validate server protocol handshakes:

```bash
scripts/mcp/doctor.sh --core
```

Validate client discovery when all three CLIs are installed:

```bash
scripts/mcp/doctor.sh --clients
```

A client-level Vuetify conflict usually means the same server is also registered in
user scope. Remove that duplicate from the user's client configuration; the generated
project entry is the reviewed definition. Personal servers and credentials belong in
`.mcp.local.json`, `.codex/local.config.toml`, `.gemini/settings.local.json` or another
ignored user configuration.

Plausible is enabled only when `PLAUSIBLE_API_KEY` is exported. Nuxt MCP becomes
available after the local frontend starts. Core validation skips both optional servers.
