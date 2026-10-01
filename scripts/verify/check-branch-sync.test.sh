#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# Git hooks export repository-local variables. The fixture must use its own
# repository, or its commits can land on the caller's branch during pre-push.
unset GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE GIT_PREFIX GIT_COMMON_DIR
fixture="$(mktemp -d)"
trap 'rm -rf -- "$fixture"' EXIT

upstream="$fixture/upstream.git"
clone="$fixture/clone"

git init --bare --initial-branch=main "$upstream" >/dev/null
git -c user.email=test@example.com -c user.name=test clone "$upstream" "$clone" >/dev/null 2>&1

git -c user.email=test@example.com -c user.name=test -C "$clone" commit --allow-empty -m init >/dev/null
git -C "$clone" push origin main >/dev/null 2>&1

run_guard() {
  O4G_SKIP_FETCH=1 "$ROOT/scripts/verify/check-branch-sync.sh" "$clone"
}

run_guard | grep -qx "branch sync guard accepted: local 'main' matches 'origin/main'"

git -c user.email=test@example.com -c user.name=test -C "$clone" commit --allow-empty -m "local-only commit" >/dev/null

guard_output="$(run_guard 2>&1 >/dev/null || true)"
if run_guard >/dev/null 2>&1; then
  echo "expected a local-only commit on main to fail the guard" >&2
  exit 1
fi

printf '%s\n' "$guard_output" | grep -q "local 'main' is 1 commit(s) ahead of 'origin/main'"

git -C "$clone" reset --hard origin/main >/dev/null

run_guard | grep -qx "branch sync guard accepted: local 'main' matches 'origin/main'"

echo "OK: branch sync guard fixtures"
