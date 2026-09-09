#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture="$(mktemp -d)"
trap 'rm -rf -- "$fixture"' EXIT

mkdir -p "$fixture/.o4g/work/ledger" "$fixture/docs/reference" \
  "$fixture/scripts/generate" "$fixture/scripts/work"
cp "$ROOT/scripts/generate/generate_roadmap.py" "$fixture/scripts/generate/"
cp "$ROOT/scripts/work/wo.py" "$fixture/scripts/work/"

printf '%s\n' \
  'apiVersion: open4goods.org/v1' \
  'kind: Project' \
  'spec:' \
  '  milestones:' \
  '    - {id: m1, order: 1}' > "$fixture/.o4g/project.yml"

printf '%s\n' \
  'apiVersion: open4goods.org/v1' \
  'kind: WorkOrder' \
  'metadata: {id: prerequisite, name: Prerequisite}' \
  'spec:' \
  '  purpose: Closed prerequisite.' \
  '  roadmapRef: m1' \
  '  state: COMPLETED' \
  '  pathScope: ["scripts/**"]' \
  '  acceptanceCriteria: [{id: AC1, statement: Done.}]' \
  > "$fixture/.o4g/work/ledger/prerequisite.yml"

printf '%s\n' \
  'apiVersion: open4goods.org/v1' \
  'kind: WorkOrder' \
  'metadata: {id: sample, name: Sample}' \
  'spec:' \
  '  purpose: Exercise the lifecycle.' \
  '  roadmapRef: m1' \
  '  state: ACCEPTED' \
  '  dependsOn: [prerequisite]' \
  '  pathScope: ["scripts/**"]' \
  '  acceptanceCriteria: [{id: AC1, statement: Lifecycle passes.}]' \
  > "$fixture/.o4g/work/sample.yml"

python3 "$fixture/scripts/generate/generate_roadmap.py" --root "$fixture" >/dev/null
python3 "$fixture/scripts/work/wo.py" --root "$fixture" next --json | grep -q '"id": "sample"'
python3 "$fixture/scripts/work/wo.py" --root "$fixture" begin sample >/dev/null
grep -q 'state: IN_PROGRESS' "$fixture/.o4g/work/sample.yml"
python3 "$fixture/scripts/work/wo.py" --root "$fixture" close sample \
  --evidence 'test:scripts/tests/workorder-tools.test.sh' >/dev/null
test -f "$fixture/.o4g/work/ledger/sample.yml"
grep -q 'state: COMPLETED' "$fixture/.o4g/work/ledger/sample.yml"

echo "OK: WorkOrder selection, transition and compaction"
