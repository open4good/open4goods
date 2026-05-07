# MCP Servers

open4goods ships Model Context Protocol configuration for assistants that can
read repository-local MCP definitions.

Two config shapes are committed because clients do not all read the same file:

- `.mcp.json` uses the common `mcpServers` shape used by Codex-style clients.
- `.vscode/mcp.json` uses the VS Code `servers` shape and is also useful as a
  template for Claude and Gemini clients.

## Servers

| Server | Runtime | Main use |
|---|---|---|
| `nuxt` | Nuxt dev server SSE at `http://localhost:3000/__mcp/sse` | Live Nuxt app context when `frontend` is running |
| `vuetify` | `npx -y @vuetify/mcp@latest` | Vuetify API and component guidance |
| `context7` | `npx -y @upstash/context7-mcp@latest` | Current library documentation lookup |
| `elasticsearch` | `docker.elastic.co/mcp/elasticsearch stdio` | Local Elasticsearch index inspection |

## Prerequisites

- Node.js and npm for `npx`-launched servers.
- Docker for the Elasticsearch MCP server.
- The local Elasticsearch service from `docker-compose.yml`.

Start the local backing services:

```bash
docker compose up -d elastic
```

Verify Elasticsearch:

```bash
curl http://localhost:9200
```

## Client Notes

Codex-style clients usually discover `.mcp.json` from the repository root.

VS Code reads `.vscode/mcp.json`.

Claude and Gemini clients can copy the server definitions from `.mcp.json` into
their user-level MCP configuration if they do not load repository-local files.
Restart the client after changing MCP config; most clients load stdio servers at
session startup.

The Elasticsearch MCP entry uses Docker host networking and
`ES_URL=http://localhost:9200`, matching this repository's local compose stack.
For a remote cluster, override `ES_URL` and provide either `ES_API_KEY` or
`ES_USERNAME` plus `ES_PASSWORD` in the client environment.
