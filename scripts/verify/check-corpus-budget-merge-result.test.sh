#!/usr/bin/env bash
# Negative test for GOU-166: a PR branch forked before another PR's changes
# landed on the base branch can be individually within budget, and the base
# branch can be individually within budget, while the tree their merge would
# produce exceeds the ceiling. Plain HEAD measurement never sees this; only
# measuring the merge result does.
#
# This fixture fails against the pre-fix check_corpus_budget.py (which ignores
# GITHUB_BASE_REF and only ever measures HEAD) and passes once the script
# measures the merge of origin/$GITHUB_BASE_REF into HEAD instead.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FIXTURE_DIR="$(mktemp -d)"
trap 'rm -rf -- "${FIXTURE_DIR}"' EXIT

UPSTREAM="${FIXTURE_DIR}/upstream.git"
REPO="${FIXTURE_DIR}/repo"

git init -q --bare --initial-branch=main "${UPSTREAM}"
git clone -q "${UPSTREAM}" "${REPO}"

git -C "${REPO}" config user.email test@example.com
git -C "${REPO}" config user.name test

mkdir -p "${REPO}/scripts/verify" "${REPO}/docs" "${REPO}/.o4g"
cp "${ROOT}/scripts/verify/check_corpus_budget.py" "${REPO}/scripts/verify/check_corpus_budget.py"

CHECK="scripts/verify/check_corpus_budget.py"

cat > "${REPO}/AGENTS.md" <<'MD'
---
title: "Agent guide"
normative: true
---

# Agent guide

1. A fixture rule, for front-matter purposes only.
MD

cat > "${REPO}/docs/a.md" <<'MD'
---
title: "A"
normative: false
---

Line one.
Line two.
MD

(cd "${REPO}" && python3 "${CHECK}" --update) > /dev/null

# Give the ceiling some margin, the way a maintainer deliberately would -- the
# measured tree at this point is well inside it. This mirrors the real
# repository's observed state (e.g. 6488/6503): room exists, but it is finite.
python3 - "${REPO}/.o4g/corpus-budget.json" <<'PY'
import json
import sys

path = sys.argv[1]
data = json.loads(open(path, encoding="utf-8").read())
data["ceilings"]["non_normative_lines"] = 6
with open(path, "w", encoding="utf-8") as handle:
    json.dump(data, handle, indent=2)
    handle.write("\n")
PY

git -C "${REPO}" add -A
git -C "${REPO}" commit -q -m "base with margin"
git -C "${REPO}" push -q origin main

# A PR branch forks here, before any other PR's changes land on main.
git -C "${REPO}" checkout -q -b pr-a

# Another PR merges to main after the fork: +2 non-normative lines.
git -C "${REPO}" checkout -q main
cat > "${REPO}/docs/b.md" <<'MD'
---
title: "B"
normative: false
---

Line one.
Line two.
MD
git -C "${REPO}" add -A
git -C "${REPO}" commit -q -m "another PR lands on main"
git -C "${REPO}" push -q origin main

# The PR branch, unaware of that change, adds its own +3 non-normative lines.
git -C "${REPO}" checkout -q pr-a
cat > "${REPO}/docs/c.md" <<'MD'
---
title: "C"
normative: false
---

Line one.
Line two.
Line three.
MD
git -C "${REPO}" add -A
git -C "${REPO}" commit -q -m "PR branch adds its own content"

# CI fetches the base ref, as the real lint-suite workflow does.
git -C "${REPO}" fetch -q origin main

# Base alone (a + b = 4 lines) is within the 6-line ceiling. Unset
# GITHUB_BASE_REF explicitly: this fixture runs inside the real lint-suite
# job, which already has it set for the actual PR, and inheriting it here
# would make the script merge-check against the fixture's own origin/main
# instead of measuring the checked-out branch alone.
if ! (cd "${REPO}" && unset GITHUB_BASE_REF && git checkout -q main && python3 "${CHECK}") > /dev/null; then
  echo "FAIL: base branch alone should be within budget"
  exit 1
fi

# The PR branch alone (a + c = 5 lines) is within the 6-line ceiling too.
git -C "${REPO}" checkout -q pr-a
if ! (cd "${REPO}" && unset GITHUB_BASE_REF && python3 "${CHECK}") > /dev/null; then
  echo "FAIL: PR branch alone should be within budget"
  exit 1
fi

# But their merge (a + b + c = 7 lines) exceeds it. This is the defect: CI
# must measure the merge result, not the PR branch's tip.
if (cd "${REPO}" && GITHUB_BASE_REF=main python3 "${CHECK}") \
    > /tmp/corpus-budget-merge-result.out 2>&1; then
  echo "FAIL: the merge of main into the PR branch exceeds the budget and should have failed"
  cat /tmp/corpus-budget-merge-result.out
  exit 1
fi

if ! grep -q "Corpus budget exceeded" /tmp/corpus-budget-merge-result.out; then
  echo "FAIL: expected 'Corpus budget exceeded' in the merge-result check's output"
  cat /tmp/corpus-budget-merge-result.out
  exit 1
fi

rm -f /tmp/corpus-budget-merge-result.out
echo "corpus-budget-merge-result test passed"
