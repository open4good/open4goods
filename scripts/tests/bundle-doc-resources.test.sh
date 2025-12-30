#!/usr/bin/env bash
set -euo pipefail

ROOT="$(pwd)"
FIXTURE_DIR="$(mktemp -d)"
cd "${FIXTURE_DIR}"

mkdir -p repo/frontend/docs repo/services/api
cat > repo/README.md <<'MD'
# Root doc
MD
cat > repo/frontend/docs/guide.md <<'MD'
# Frontend guide
MD
cat > repo/services/api/usage.prompt <<'PR'
Prompt content
PR

touch repo/BOGUS.txt
jar cf repo/sample.jar -C repo BOGUS.txt

cd "${ROOT}"

scripts/bundle-doc-resources.sh "${FIXTURE_DIR}/repo" "${FIXTURE_DIR}/repo/target/classpath-docs" "${FIXTURE_DIR}/repo/sample.jar"

STAGED_DIR="${FIXTURE_DIR}/repo/target/classpath-docs/docs"
[[ -f "${STAGED_DIR}/README.md" ]] || { echo "README not staged"; exit 1; }
[[ -f "${STAGED_DIR}/frontend/docs/guide.md" ]] || { echo "guide not staged"; exit 1; }
[[ -f "${STAGED_DIR}/services/api/usage.prompt" ]] || { echo "prompt not staged"; exit 1; }

if ! jar tf "${FIXTURE_DIR}/repo/sample.jar" | grep -q "docs/README.md"; then
  echo "Docs not embedded into jar"
  exit 1
fi
if ! jar tf "${FIXTURE_DIR}/repo/sample.jar" | grep -q "docs/frontend/docs/guide.md"; then
  echo "Nested doc not embedded"
  exit 1
fi
if ! jar tf "${FIXTURE_DIR}/repo/sample.jar" | grep -q "docs/services/api/usage.prompt"; then
  echo "Prompt not embedded"
  exit 1
fi

echo "bundle-doc-resources test passed"
