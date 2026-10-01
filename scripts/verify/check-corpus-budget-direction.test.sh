#!/usr/bin/env bash
# Regression fixture for --assert-no-ceiling-increase: a PR that raises a ceiling
# must fail, a PR that lowers one must pass, and an unreadable ref must fail loud
# rather than skip silently (GOU-162).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture="$(mktemp -d)"
trap 'rm -rf -- "$fixture"' EXIT

mkdir -p "$fixture/scripts/verify" "$fixture/.o4g"
cp "$ROOT/scripts/verify/check_corpus_budget.py" "$fixture/scripts/verify/check_corpus_budget.py"

git -C "$fixture" init --quiet --initial-branch=main
git -C "$fixture" config user.email test@example.com
git -C "$fixture" config user.name test

write_budget() {
  cat >"$fixture/.o4g/corpus-budget.json" <<JSON
{
  "ceilings": {
    "non_normative_lines": $1
  }
}
JSON
}

write_budget 100
git -C "$fixture" add -A
git -C "$fixture" commit --quiet -m baseline
git -C "$fixture" tag baseline

run_check() {
  python3 "$fixture/scripts/verify/check_corpus_budget.py" --assert-no-ceiling-increase "$1"
}

write_budget 120
git -C "$fixture" add -A
git -C "$fixture" commit --quiet -m raise

if output="$(run_check baseline 2>&1)"; then
  echo "expected a raised ceiling to be rejected against baseline" >&2
  printf '%s\n' "$output" >&2
  exit 1
fi
printf '%s\n' "$output" | grep -q "non_normative_lines: 120 exceeds 100 at baseline"

write_budget 90
git -C "$fixture" add -A
git -C "$fixture" commit --quiet -m lower

run_check baseline | grep -qx "OK: no corpus ceiling rose against baseline."

if output="$(run_check does-not-exist 2>&1)"; then
  echo "expected an unreadable ref to fail instead of skipping silently" >&2
  printf '%s\n' "$output" >&2
  exit 1
fi
printf '%s\n' "$output" | grep -q "Cannot read .o4g/corpus-budget.json at 'does-not-exist'"

echo "OK: corpus budget direction fixtures"
