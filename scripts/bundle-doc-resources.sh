#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${1:-.}"
STAGING_DIR="${2:-target/classpath-docs}"
shift 2 || true
JAR_PATHS=("$@")

# Normalize staging directory
mkdir -p "${STAGING_DIR}/docs"
rm -rf "${STAGING_DIR}/docs"
mkdir -p "${STAGING_DIR}/docs"

pushd "${ROOT_DIR}" >/dev/null

find_command=(find . \(
  -path "./.git" -o
  -path "./.mvn" -o
  -path "./frontend/node_modules" -o
  -path "./frontend/.output" -o
  -path "./frontend/.nuxt" -o
  -name target -o
  -name .gradle
\) -prune -o -type f \( -name "*.md" -o -name "*.prompt" \) -print0)

if ! rsync --version >/dev/null 2>&1; then
  echo "rsync is required to stage documentation resources" >&2
  exit 1
fi

# shellcheck disable=SC2046
"${find_command[@]}" | rsync -a --from0 --files-from=- ./ "${STAGING_DIR}/docs"

popd >/dev/null

if [[ ${#JAR_PATHS[@]} -eq 0 ]]; then
  echo "No jar paths provided; staging docs only."
  exit 0
fi

if ! command -v jar >/dev/null 2>&1; then
  echo "jar command not found; ensure JDK is installed." >&2
  exit 1
fi

for jar_path in "${JAR_PATHS[@]}"; do
  if [[ ! -f "${jar_path}" ]]; then
    echo "Warning: jar not found at ${jar_path}; skipping." >&2
    continue
  fi

  echo "Embedding documentation resources into ${jar_path}" >&2
  jar uf "${jar_path}" -C "${STAGING_DIR}" .
done
