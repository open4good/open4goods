#!/usr/bin/env bash
# Run the repository lint suite. Pass --fix to apply supported rewrites.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec python3 "$REPO_ROOT/scripts/python/lint_suite.py" "$@"
