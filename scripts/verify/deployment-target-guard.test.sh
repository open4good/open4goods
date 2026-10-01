#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture="$(mktemp -d)"
trap 'rm -rf -- "$fixture"' EXIT

fingerprint_a="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
fingerprint_b="abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
target_marker="$fixture/deployment-target"
cluster_marker="$fixture/elasticsearch-cluster-fingerprint"

printf '%s\n' beta > "$target_marker"
printf '%s\n' "$fingerprint_a" > "$cluster_marker"

run_guard() {
  O4G_EXPECTED_TARGET="$1" \
    O4G_EXPECTED_CLUSTER_FINGERPRINT="$2" \
    O4G_TARGET_MARKER_FILE="$target_marker" \
    O4G_CLUSTER_FINGERPRINT_FILE="$cluster_marker" \
    "$ROOT/scripts/verify/deployment-target-guard.sh"
}

run_guard beta "$fingerprint_a" | grep -qx 'deployment target guard accepted beta'

if run_guard prod "$fingerprint_a" >/dev/null 2>&1; then
  echo "expected wrong-host target fixture to fail" >&2
  exit 1
fi

if run_guard beta "$fingerprint_b" >/dev/null 2>&1; then
  echo "expected wrong-cluster fixture to fail" >&2
  exit 1
fi

# GOU-160: these workflows have no CI-driven auto-deploy job today. Beta/prod promotion
# runs through scripts/deploy (systemd runtime install + gate_proof_seal.py), not a GitHub
# Actions `deploy`/`release` step, and production is deliberately frozen pending GOU-53/GOU-31.
# This asserts that current, intentional shape so a `deploy`/`release` job can't be added back
# silently without updating this guard (and re-adding the target-identity check it used to pin).
python3 - "$ROOT" <<'PY'
import sys
from pathlib import Path

import yaml

root = Path(sys.argv[1])

def load(name):
    return yaml.load((root / '.github' / 'workflows' / name).read_text(), Loader=yaml.BaseLoader)

beta = load('testAndPublishBeta.yml')
assert set(beta['jobs']) == {'build'}, (
    "testAndPublishBeta.yml grew a job beyond 'build': if this is a new CI-driven deploy job, "
    "it needs a 'Verify beta target identity' step and this guard must be updated to check for it"
)

frontend = load('frontend-ci.yml')
assert set(frontend['jobs']) == {'build'}, (
    "frontend-ci.yml grew a job beyond 'build': if this is a new CI-driven deploy job, "
    "it needs a 'Verify beta target identity' step and this guard must be updated to check for it"
)

production = load('releaseDeployProd.yml')
assert set(production['jobs']) == {'frozen'}, (
    "releaseDeployProd.yml grew a job beyond 'frozen': production promotion requires a fresh "
    "owner decision after GOU-53/GOU-31 (see GOU-94); if that decision has landed, this guard "
    "must be updated to assert the new 'release' job runs 'Verify production target identity'"
)
frozen_job = production['jobs']['frozen']
assert any('exit 1' in step.get('run', '') for step in frozen_job['steps']), (
    "the 'frozen' job must keep unconditionally refusing production promotion until GOU-94 lands"
)
PY

echo "OK: deployment target and shared beta deployment guard fixtures"
