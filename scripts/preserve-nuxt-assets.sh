#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: preserve-nuxt-assets.sh STAGING_BUNDLE [SOURCE_BUNDLE...]

Copies missing files from each source bundle's .output/public/_nuxt directory
into the staging bundle. Existing staging files are never overwritten, so the
current build and its builds/latest.json stay authoritative.
USAGE
}

if [[ $# -lt 2 ]]; then
  usage
  exit 2
fi

staging_bundle="$1"
shift

staging_nuxt="${staging_bundle%/}/.output/public/_nuxt"
mkdir -p "${staging_nuxt}"

copied=0
for source_bundle in "$@"; do
  source_nuxt="${source_bundle%/}/.output/public/_nuxt"
  if [[ ! -d "${source_nuxt}" ]]; then
    echo "Skipping missing Nuxt asset source: ${source_nuxt}"
    continue
  fi

  while IFS= read -r -d '' source_file; do
    relative_path="${source_file#"${source_nuxt}/"}"
    target_file="${staging_nuxt}/${relative_path}"
    if [[ -e "${target_file}" ]]; then
      continue
    fi
    mkdir -p "$(dirname "${target_file}")"
    cp -p "${source_file}" "${target_file}"
    copied=$((copied + 1))
  done < <(find "${source_nuxt}" -type f -print0)
done

echo "Preserved ${copied} missing Nuxt asset file(s) in ${staging_nuxt}"
