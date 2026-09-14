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
  '  executionPhase: DEVELOPMENT' \
  '  priority: 100' \
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

python3 - "$fixture" <<'PY'
import json
import subprocess
import sys
from pathlib import Path

import yaml

root = Path(sys.argv[1])
orders = root / '.o4g/work'

def write_order(identifier, *, phase='DEVELOPMENT', priority=100, state='ACCEPTED', dependencies=None, blockers=None):
    payload = {
        'apiVersion': 'open4goods.org/v1', 'kind': 'WorkOrder',
        'metadata': {'id': identifier, 'name': identifier},
        'spec': {'purpose': 'Phase and dependency regression fixture.', 'roadmapRef': 'm1',
                 'state': state, 'executionPhase': phase, 'priority': priority,
                 'dependsOn': dependencies or [], 'externalBlockers': blockers or [],
                 'pathScope': ['scripts/**'], 'acceptanceCriteria': [{'id': 'AC1', 'statement': 'Verified.'}]}
    }
    (orders / f'{identifier}.yml').write_text(yaml.safe_dump(payload))

def cli(*args, ok=True):
    result = subprocess.run([sys.executable, str(root / 'scripts/work/wo.py'), '--root', str(root), *args],
                            text=True, capture_output=True)
    assert (result.returncode == 0) == ok, (args, result.stdout, result.stderr)
    return result

write_order('later', priority=200)
write_order('first', priority=10)
write_order('active', priority=900, state='IN_PROGRESS')
write_order('prod', phase='PRODUCTION', priority=0)
write_order('waiting', dependencies=['first'])
write_order('external', blockers=['AC1: owner mapping review'])
write_order('blocked-state', state='BLOCKED')
selected = json.loads(cli('next', '--json').stdout)
assert [entry['id'] for entry in selected] == ['active', 'first', 'later'], selected
all_development = json.loads(cli('next', '--all', '--json', '--limit', '100').stdout)
assert 'prod' not in [entry['id'] for entry in all_development]
all_phases = json.loads(cli('next', '--phase', 'ALL', '--all', '--json', '--limit', '100').stdout)
assert next(entry for entry in all_phases if entry['id'] == 'prod')['availability'] == 'AWAITING_OWNER_ORDER'
assert next(entry for entry in all_phases if entry['id'] == 'blocked-state')['availability'] == 'BLOCKED'
cli('begin', 'waiting', ok=False)
cli('begin', 'external', ok=False)
cli('begin', 'prod', ok=False)
cli('begin', 'prod', '--authorization-ref', 'test-only-owner-order')
cli('close', 'prod', '--evidence', 'test:verified', ok=False)
cli('close', 'prod', '--evidence', 'test:verified', '--authorization-ref', 'test-only-owner-order')
assert 'owner-order:test-only-owner-order' in (orders / 'ledger/prod.yml').read_text()
cli('begin', 'first')
cli('block', 'first', '--evidence', 'note:AC1 external resource unavailable')
info = yaml.safe_load(cli('status', 'first').stdout)
assert info['blockers'] == ['note:AC1 external resource unavailable']
cli('begin', 'first', ok=False)

write_order('future', phase='POST_PRODUCTION')
write_order('invalid-edge', dependencies=['future'])
assert 'later execution phase' in cli('next', ok=False).stderr
(orders / 'invalid-edge.yml').unlink()
write_order('missing', dependencies=['does-not-exist'])
assert 'missing dependency' in cli('next', ok=False).stderr
(orders / 'missing.yml').unlink()
write_order('cycle-a', dependencies=['cycle-b'])
write_order('cycle-b', dependencies=['cycle-a'])
assert 'cycle' in cli('next', ok=False).stderr
(orders / 'cycle-a.yml').unlink()
(orders / 'cycle-b.yml').unlink()
write_order('invalid-phase', phase='PROD_TYPO')
assert 'executionPhase' in cli('next', ok=False).stderr
(orders / 'invalid-phase.yml').unlink()
write_order('invalid-priority', priority=-1)
assert 'priority' in cli('next', ok=False).stderr
(orders / 'invalid-priority.yml').unlink()
subprocess.run([sys.executable, str(root / 'scripts/generate/generate_roadmap.py'), '--root', str(root)], check=True,
               capture_output=True)
roadmap = (root / 'docs/reference/roadmap.md').read_text()
assert 'AC1 external resource unavailable' in roadmap
assert 'POST_PRODUCTION' in roadmap
PY

echo "OK: WorkOrder lifecycle, phase gates, priority, external blockers and graph validation"
