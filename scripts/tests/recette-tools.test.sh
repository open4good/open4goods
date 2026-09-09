#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
run_id="tool-test-$$"
run_dir="$ROOT/artifacts/recette/$run_id"
trap 'rm -rf -- "$run_dir"' EXIT

python3 "$ROOT/scripts/recette/board.py" resume --run "$run_id" --mode desktop --locale fr \
  | grep -q '^home:desktop:fr$'
python3 "$ROOT/scripts/recette/board.py" claim --run "$run_id" home:desktop:fr
python3 "$ROOT/scripts/recette/board.py" verdict --run "$run_id" home:desktop:fr \
  --status PASSED --note 'fixture passed' --evidence 'fixture.png'
python3 "$ROOT/scripts/recette/board.py" matrix --run "$run_id" | grep -q 'PASSED,1'
python3 "$ROOT/scripts/recette/board.py" report --run "$run_id" >/dev/null
test -f "$run_dir/report.md"

printf '%s\n' 'ordinary startup' 'ERROR missing translation for key home.title' > "$run_dir/front-api.log"
printf '%s\n' 'render complete' > "$run_dir/frontend-ssr.log"
printf '%s\n' '{"front-api.log":0,"frontend-ssr.log":0}' > "$run_dir/offsets.json"
if python3 "$ROOT/scripts/recette/log-audit.py" --run "$run_id" >/dev/null; then
  echo "expected log audit to detect the fixture error" >&2
  exit 1
fi
jq -e '.findings | length == 1' "$run_dir/log-audit.json" >/dev/null

echo "OK: recette board, report and incremental log audit"
