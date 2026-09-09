#!/usr/bin/env bash
set -euo pipefail
socket="${DOCKER_HOST_SOCKET:-/var/run/docker.sock}"
if [ ! -S "$socket" ]; then
  echo "Docker socket is unavailable: $socket" >&2
  exit 1
fi
exec docker run -i --rm \
  -e DOCKER_MCP_SERVER_READONLY=true \
  -v "$socket:/var/run/docker.sock" \
  ghcr.io/l337-org/docker-mcp-server@sha256:ee5ed58fd09c1ea98d7e8233337d8428b9ff5fddf223221df36b12952fc943d5
