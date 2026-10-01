#!/usr/bin/env bash
# Executable behavior tests for the shared-build-host preflight gate and the
# full-run lock: each case runs the real preflight()/with_full_run_lock()
# functions against a scratch environment and asserts pass/fail, instead of
# grepping the script for symbol names.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FUNCTIONS="$(mktemp)"
trap 'rm -rf "$FUNCTIONS" "$SCRATCH"' EXIT
SCRATCH="$(mktemp -d)"

# Strip the trailing command dispatch so sourcing only defines functions.
awk '/^command_name="\$\{1:-\}"/{exit} {print}' "$ROOT/scripts/local/open4goods.sh" > "$FUNCTIONS"

fail() { echo "FAIL: $1" >&2; exit 1; }

run_preflight() {
  # Fresh subshell per case: preflight()/load_env() mutate global state via
  # `local`-scoped vars but load_env exports into the environment, so a
  # subshell keeps cases from bleeding into each other.
  (
    ROOT="$ROOT"
    # shellcheck disable=SC1090
    source "$FUNCTIONS"
    # Supply a safe, repeatable host budget; each case still executes the real
    # path and port validation without requiring a Docker daemon in CI.
    docker() { [ "$1" = info ] && printf '["name=rootless"]\n'; }
    ss() { :; }
    cgroup_effective_max() { if [ "$1" = memory.max ]; then echo 34359738368; else echo 4096; fi; }
    cgroup_effective_headroom_bytes() { echo 17179869184; }
    cgroup_cpu_millicores_max() { echo 8000; }
    ROOT="$SCRATCH/repo"
    LOCAL_ROOT="$ROOT/.local"
    ENV_FILE="$ROOT/.env.local"
    mkdir -p "$LOCAL_ROOT/data" "$LOCAL_ROOT/config"
    cp "$1" "$ENV_FILE"
    preflight "${2:-standard}"
  )
}

base_env() {
  local out="$SCRATCH/env.$1"
  cat > "$out" <<'EOF'
O4G_PORT_API=4100
O4G_PORT_UI=4101
O4G_PORT_ADMIN=4102
O4G_PORT_FRONT_API=4103
O4G_PORT_B2B_API=4104
O4G_PORT_EXPOSED_DOCS=4105
O4G_PORT_GEOCODE=4106
O4G_PORT_FRONTEND=4107
O4G_PORT_B2B_FRONTEND=4108
O4G_PORT_ELASTICSEARCH=4150
O4G_PORT_REDIS=4151
O4G_PORT_POSTGRES=4152
O4G_PORT_KIBANA=4153
O4G_PORT_XWIKI=4154
O4G_LOCAL_POSTGRES_PASSWORD=dev-only
O4G_LOCAL_XWIKI_DB_PASSWORD=dev-only
O4G_LOCAL_XWIKI_ROOT_PASSWORD=dev-only
FRONT_SECURITY_SHARED_TOKEN=dev-only
FRONT_SECURITY_JWT_SECRET=dev-only-32-bytes-minimum-placeholder-x
B2B_JWT_SECRET=dev-only
O4G_LOCAL_ADMIN_KEY=dev-only
EOF
  printf 'O4G_LOCAL_DATA_ROOT=%s\n' "$SCRATCH/issue-GOU-133/data" >> "$out"
  printf 'O4G_SHARED_HOST_APPROVED_ROOTS=%s:%s\n' "$SCRATCH/repo/.local" "$SCRATCH/issue-GOU-133" >> "$out"
  printf '%s\n' "$out"
}

mkdir -p "$SCRATCH/repo/.local/data" "$SCRATCH/repo/.local/config"
mkdir -p "$SCRATCH/issue-GOU-133/data"

# --- case: a valid buildhost env passes ---
env_ok="$(base_env ok)"
run_preflight "$env_ok" >/dev/null || fail "a fully valid shared-host env was rejected"
echo "ok: valid env passes preflight"

# --- case: duplicate app port is rejected ---
env_dup="$(base_env dup)"
sed -i 's/^O4G_PORT_UI=.*/O4G_PORT_UI=4100/' "$env_dup"
run_preflight "$env_dup" >/dev/null 2>&1 && fail "duplicate port was accepted" || true
echo "ok: duplicate port rejected"

# --- case: port outside the assigned block is rejected ---
env_range="$(base_env range)"
sed -i 's/^O4G_PORT_UI=.*/O4G_PORT_UI=9999/' "$env_range"
run_preflight "$env_range" >/dev/null 2>&1 && fail "out-of-range port was accepted" || true
echo "ok: out-of-range port rejected"

# --- case: a CHANGE_ME secret is rejected (incomplete config) ---
env_incomplete="$(base_env incomplete)"
sed -i 's/^O4G_LOCAL_ADMIN_KEY=.*/O4G_LOCAL_ADMIN_KEY=CHANGE_ME/' "$env_incomplete"
run_preflight "$env_incomplete" >/dev/null 2>&1 && fail "a CHANGE_ME placeholder secret was accepted" || true
echo "ok: incomplete config (CHANGE_ME secret) rejected"

# --- case: a symlink escape out of the approved data root is rejected ---
env_symlink="$(base_env symlink)"
ln -sfn /etc "$SCRATCH/repo/.local/escape"
echo "O4G_LOCAL_DATA_ROOT=$SCRATCH/repo/.local/escape" >> "$env_symlink"
run_preflight "$env_symlink" >/dev/null 2>&1 && fail "a symlink escaping the approved root was accepted" || true
echo "ok: symlink escape outside approved roots rejected"

# --- case: Docker bind mounts inside the worktree are rejected ---
env_worktree="$(base_env worktree)"
echo "O4G_LOCAL_DATA_ROOT=$SCRATCH/repo/.local/data" >> "$env_worktree"
run_preflight "$env_worktree" >/dev/null 2>&1 && fail "Docker data inside the worktree was accepted" || true
echo "ok: Docker data inside the worktree rejected"

# --- case: a git worktree registered inside the repo root is rejected ---
env_nested_wt="$(base_env nested-wt)"
git -C "$SCRATCH/repo" init -q
git -C "$SCRATCH/repo" -c user.name=test -c user.email=test@example.com \
  commit -q --allow-empty -m init
git -C "$SCRATCH/repo" worktree add -q -b gou-nested-test \
  "$SCRATCH/repo/.worktrees/gou-nested-test" >/dev/null
run_preflight "$env_nested_wt" >/dev/null 2>&1 && fail "a git worktree inside the repo root was accepted" || true
echo "ok: git worktree inside the repo root rejected"
git -C "$SCRATCH/repo" worktree remove --force "$SCRATCH/repo/.worktrees/gou-nested-test"
run_preflight "$env_nested_wt" >/dev/null || fail "preflight stayed rejected after removing the in-tree worktree"
echo "ok: preflight passes again once the in-tree worktree is removed"

# --- case: the full-import floor is stricter than the standard floor ---
env_ok2="$(base_env floor)"
standard_msg="$(run_preflight "$env_ok2" standard)"
full_import_msg="$(run_preflight "$env_ok2" full-import)"
echo "$standard_msg" | grep -q 'disk>=20GiB' || fail "standard floor did not default to 20GiB: $standard_msg"
echo "$full_import_msg" | grep -q 'disk>=60GiB' || fail "full-import floor did not raise the disk requirement: $full_import_msg"
echo "ok: full-import preflight enforces a stricter disk floor than standard"

# --- case: the full-run lock lives outside any worktree and serializes ---
lock_dir="$SCRATCH/shared-lock"
(
  # shellcheck disable=SC1090
  source "$FUNCTIONS"
  # shellcheck disable=SC2034  # consumed by with_full_run_lock() in $FUNCTIONS
  SHARED_LOCK_ROOT="$lock_dir"
  with_full_run_lock sleep 3 &
  first=$!
  sleep 1
  if with_full_run_lock true 2>/dev/null; then
    echo "SECOND_ACQUIRED"
  else
    echo "SECOND_REJECTED"
  fi
  wait "$first"
) | grep -q SECOND_REJECTED || fail "a concurrent full run was not serialized by the shared lock"
[ -f "$lock_dir/full-run.lock" ] || fail "the lock file was not created under the dedicated SHARED_LOCK_ROOT"
echo "ok: full-run lock serializes concurrent runs from a dedicated per-user path"

echo "OK: preflight gate and full-run lock behavior"
