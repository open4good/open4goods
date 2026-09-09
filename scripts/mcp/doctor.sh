#!/usr/bin/env bash
# Validate portable MCP projections, core protocol handshakes and optional client discovery.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

mode="${1:---core}"
case "$mode" in
  --core|--clients|--all) ;;
  *) echo "usage: $0 [--core|--clients|--all]" >&2; exit 2 ;;
esac

python3 scripts/generate/generate_mcp_configs.py --check
if rg -n '/home/[^$]|/Users/[^$]' .mcp.json .codex/config.toml .gemini/settings.json .vscode/mcp.json; then
  echo "FAIL: a generated MCP projection contains a machine-absolute path" >&2
  exit 1
fi

if [ "$mode" = "--core" ] || [ "$mode" = "--all" ]; then
  python3 scripts/mcp/smoke.py --core
fi

if [ "$mode" = "--clients" ] || [ "$mode" = "--all" ]; then
  required=(javalens maven-deps maven-tools vuetify docker)
  for client in claude codex gemini; do
    if ! command -v "$client" >/dev/null 2>&1; then
      echo "FAIL: $client CLI is not installed" >&2
      exit 1
    fi
    output="$($client mcp list 2>&1)"
    for server in "${required[@]}"; do
      if ! grep -Fq "$server" <<<"$output"; then
        echo "FAIL: $client does not discover $server" >&2
        exit 1
      fi
    done
    echo "OK $client: core server names discovered"
  done
fi
