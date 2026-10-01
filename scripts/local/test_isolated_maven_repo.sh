#!/usr/bin/env bash
# Executable behavior tests for the isolated Maven repository purge and
# free-space guard (GOU-175): each case runs the real script against a
# scratch environment, not a grep of its source.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT="$ROOT/scripts/local/isolated-maven-repo.sh"
SCRATCH="$(mktemp -d)"
trap 'rm -rf "$SCRATCH"' EXIT

fail() { echo "FAIL: $1" >&2; exit 1; }

FAKE_SHARED_M2="$SCRATCH/fake-shared-m2"
mkdir -p "$FAKE_SHARED_M2/org/example/1.0"
echo fake-jar > "$FAKE_SHARED_M2/org/example/1.0/example-1.0.jar"

# --- case: a stale run directory is purged, an active one survives ---
RUN_ROOT="$SCRATCH/run-root"
mkdir -p "$RUN_ROOT/stale-run/repository"
touch -d '48 hours ago' "$RUN_ROOT/stale-run/.last-used"
mkdir -p "$RUN_ROOT/active-run/repository"
touch "$RUN_ROOT/active-run/.last-used"

O4G_SHARED_M2_REPOSITORY="$FAKE_SHARED_M2" \
O4G_LOCAL_M2_ROOT="$RUN_ROOT" \
O4G_LOCAL_M2_MAX_AGE_HOURS=24 \
  "$SCRIPT" new-run >/dev/null

[ -d "$RUN_ROOT/stale-run" ] && fail "a run directory older than the max age was not purged"
[ -d "$RUN_ROOT/active-run" ] || fail "a recently-used run directory was purged"
[ -d "$RUN_ROOT/new-run/repository" ] || fail "the requested run directory was not bootstrapped"
echo "ok: stale run directory purged, active run directory survives"

# --- case: a run directory with no marker falls back to its own mtime ---
RUN_ROOT2="$SCRATCH/run-root-no-marker"
mkdir -p "$RUN_ROOT2/no-marker-run/repository"
touch -d '48 hours ago' "$RUN_ROOT2/no-marker-run"

O4G_SHARED_M2_REPOSITORY="$FAKE_SHARED_M2" \
O4G_LOCAL_M2_ROOT="$RUN_ROOT2" \
O4G_LOCAL_M2_MAX_AGE_HOURS=24 \
  "$SCRIPT" other-run >/dev/null

[ -d "$RUN_ROOT2/no-marker-run" ] && fail "a markerless stale run directory was not purged"
echo "ok: markerless run directory falls back to directory mtime for purge"

# --- case: reusing a run id refreshes its marker so it is not purged next time ---
RUN_ROOT3="$SCRATCH/run-root-reuse"
O4G_SHARED_M2_REPOSITORY="$FAKE_SHARED_M2" \
O4G_LOCAL_M2_ROOT="$RUN_ROOT3" \
  "$SCRIPT" reused-run >/dev/null
touch -d '48 hours ago' "$RUN_ROOT3/reused-run/.last-used"
O4G_SHARED_M2_REPOSITORY="$FAKE_SHARED_M2" \
O4G_LOCAL_M2_ROOT="$RUN_ROOT3" \
O4G_LOCAL_M2_MAX_AGE_HOURS=24 \
  "$SCRIPT" reused-run >/dev/null
[ -d "$RUN_ROOT3/reused-run/repository" ] || fail "a reused run id was purged instead of refreshed"
find "$RUN_ROOT3/reused-run/.last-used" -mmin -5 >/dev/null || fail "reusing a run id did not refresh its marker"
echo "ok: bootstrapping an existing run id refreshes its marker instead of purging it"

# --- case: the free-space guard refuses to bootstrap below the threshold ---
RUN_ROOT4="$SCRATCH/run-root-space"
if O4G_SHARED_M2_REPOSITORY="$FAKE_SHARED_M2" \
   O4G_LOCAL_M2_ROOT="$RUN_ROOT4" \
   O4G_LOCAL_M2_MIN_FREE_GIB=100000000 \
     "$SCRIPT" too-big-run >/dev/null 2>"$SCRATCH/guard.err"; then
  fail "bootstrap succeeded despite an impossible free-space floor"
fi
grep -q "refusing to start" "$SCRATCH/guard.err" || fail "the free-space guard did not print an explicit refusal message"
[ -d "$RUN_ROOT4/too-big-run" ] && fail "a run directory was created despite the free-space guard rejecting it"
echo "ok: free-space guard refuses to bootstrap below the configured floor"

echo "OK: isolated Maven repository purge and free-space guard"
