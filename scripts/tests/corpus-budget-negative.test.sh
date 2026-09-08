#!/usr/bin/env bash
# Negative test for governance-kit-bootstrap AC2: ceilings are written from the
# measured tree by --update, and adding prose to a non-normative document fails
# the check. Builds a minimal fixture repo (not the real one) so the test can
# freely mutate a doc without touching tracked files.
set -euo pipefail

ROOT="$(pwd)"
FIXTURE_DIR="$(mktemp -d)"
trap 'rm -rf "${FIXTURE_DIR}"' EXIT

mkdir -p "${FIXTURE_DIR}/scripts/verify" "${FIXTURE_DIR}/docs" "${FIXTURE_DIR}/.o4g"
cp "${ROOT}/scripts/verify/check_corpus_budget.py" "${FIXTURE_DIR}/scripts/verify/check_corpus_budget.py"

cat > "${FIXTURE_DIR}/AGENTS.md" <<'MD'
---
title: "Agent guide"
normative: true
audience: PROJECT_SCOPED
---

# Agent guide

1. A fixture rule, for decision-numbering purposes only.
MD

cat > "${FIXTURE_DIR}/docs/00-canonical-decisions.md" <<'MD'
---
title: "Canonical decisions"
normative: true
audience: PROJECT_SCOPED
---

# Canonical decisions

1. A fixture rule.
MD

cat > "${FIXTURE_DIR}/docs/example.md" <<'MD'
---
title: "Example"
normative: false
audience: PROJECT_SCOPED
---

# Example

A short, non-normative explanation.
MD

CHECK="${FIXTURE_DIR}/scripts/verify/check_corpus_budget.py"

# Baseline: --update writes ceilings from the current (small) tree and exits 0.
(cd "${FIXTURE_DIR}" && python3 "${CHECK}" --update) > /dev/null

# The unmodified fixture is within its own just-written budget.
if ! (cd "${FIXTURE_DIR}" && python3 "${CHECK}") > /dev/null; then
  echo "FAIL: fixture should pass its own freshly-written budget"
  exit 1
fi

# Adding prose to the non-normative doc pushes non_normative_lines past the
# ceiling that --update just recorded.
for _ in $(seq 1 20); do
  echo "Another explanatory sentence that adds to the non-normative line count." \
    >> "${FIXTURE_DIR}/docs/example.md"
done

if (cd "${FIXTURE_DIR}" && python3 "${CHECK}") > /tmp/corpus-budget-negative.out 2>&1; then
  echo "FAIL: adding prose to a non-normative document should have failed the check"
  cat /tmp/corpus-budget-negative.out
  exit 1
fi

if ! grep -q "Corpus budget exceeded" /tmp/corpus-budget-negative.out; then
  echo "FAIL: expected 'Corpus budget exceeded' in the check's output"
  cat /tmp/corpus-budget-negative.out
  exit 1
fi

rm -f /tmp/corpus-budget-negative.out
echo "corpus-budget-negative test passed"
