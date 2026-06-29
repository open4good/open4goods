# MCP Servers

open4goods ships Model Context Protocol configuration for assistants that can
read repository-local MCP definitions.

Multiple config shapes are committed because clients do not all read the same
file:

- `.mcp.json` uses the common `mcpServers` shape used by Codex-style clients.
- `.codex/config.toml` uses the native Codex CLI project config shape.
- `.gemini/settings.json` uses the Gemini CLI project settings shape.
- `.vscode/mcp.json` uses the VS Code `servers` shape and is also useful as a
  template for other MCP clients.

## Servers

| Server | Runtime | Main use |
|---|---|---|
| `nuxt` | Nuxt dev server SSE at `http://localhost:3000/__mcp/sse` | Live Nuxt app context when `frontend` is running |
| `vuetify` | `npx -y @vuetify/mcp@latest` | Vuetify API and component guidance |
| `context7` | `npx -y @upstash/context7-mcp@latest` | Current library documentation lookup |
| `plausible` | `npx -y @icjia/plausible-mcp@0.1.5` | Plausible Analytics stats for `nudger.fr` on `https://plausible.nudger.fr` |
| `elasticsearch` | `docker.elastic.co/mcp/elasticsearch stdio` | Local Elasticsearch index inspection |

## Prerequisites

- Node.js and npm for `npx`-launched servers.
- Node.js 22 or newer for the Plausible MCP server.
- Docker for the Elasticsearch MCP server.
- The local Elasticsearch service from `docker-compose.yml`.
- `PLAUSIBLE_API_KEY` exported in the shell or configured in the MCP client
  environment for Plausible. Do not commit this key.

Start the local backing services:

```bash
docker compose up -d elastic
```

Verify Elasticsearch:

```bash
curl http://localhost:9200
```

## Client Notes

Codex CLI reads `.codex/config.toml` for trusted projects. The Plausible entry
forwards `PLAUSIBLE_API_KEY` from the local environment and sets the non-secret
defaults:

```bash
export PLAUSIBLE_API_KEY=your-api-key
codex
```

Codex-style clients that read repository-local JSON can also use `.mcp.json`.

VS Code reads `.vscode/mcp.json`.

Gemini CLI reads `.gemini/settings.json` from the project. Start Gemini from a
shell that exports `PLAUSIBLE_API_KEY`.

Claude CLI can either load the repository `.mcp.json` as project configuration
or register the same server at user scope:

```bash
claude mcp add plausible -s user \
  -e PLAUSIBLE_BASE_URL=https://plausible.nudger.fr \
  -e PLAUSIBLE_DEFAULT_SITE=nudger.fr \
  -e PLAUSIBLE_API_KEY="$PLAUSIBLE_API_KEY" \
  -- npx -y @icjia/plausible-mcp@0.1.5
```

Restart the client after changing MCP config; most clients load stdio servers at
session startup.

The Elasticsearch MCP entry uses Docker host networking and
`ES_URL=http://localhost:9200`, matching this repository's local compose stack.
For a remote cluster, override `ES_URL` and provide either `ES_API_KEY` or
`ES_USERNAME` plus `ES_PASSWORD` in the client environment.
