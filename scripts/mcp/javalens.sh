#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export JAVA_PROJECT_PATH="$repo_root"
exec npx -y javalens-mcp@1.5.1
