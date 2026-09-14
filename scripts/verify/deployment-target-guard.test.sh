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

python3 - "$ROOT" <<'PY'
import sys
from pathlib import Path

import yaml

root = Path(sys.argv[1])

def load(name):
    return yaml.load((root / '.github' / 'workflows' / name).read_text(), Loader=yaml.BaseLoader)

beta = load('testAndPublishBeta.yml')
beta_job = beta['jobs']['deploy']
assert beta_job['environment'] == 'beta'
assert beta_job['concurrency']['group'] == 'deploy-beta'
assert beta_job['concurrency']['cancel-in-progress'] == 'false'
assert any(step.get('name') == 'Verify beta target identity' for step in beta_job['steps'])

frontend = load('frontend-ci.yml')
frontend_deploy = frontend['jobs']['deploy']
assert frontend_deploy['environment'] == 'beta'
assert frontend_deploy['concurrency']['group'] == 'deploy-beta'
assert frontend_deploy['concurrency']['cancel-in-progress'] == 'false'
assert any(step.get('name') == 'Verify beta target identity' for step in frontend_deploy['steps'])

production = load('releaseDeployProd.yml')
assert 'push' not in production['on']
production_job = production['jobs']['release']
assert production_job['environment'] == 'prod'
assert any(step.get('name') == 'Verify production target identity' for step in production_job['steps'])
PY

echo "OK: deployment target and shared beta deployment guard fixtures"
