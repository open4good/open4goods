#!/usr/bin/env bash
# Build a self-verifying release bundle for the five Java and two Nuxt artifacts.
set -euo pipefail

usage() {
  echo "usage: $0 --release SHA --contract-version VERSION --output DIRECTORY \\" >&2
  echo "  --sbadmin JAR --api JAR --front-api JAR --ui JAR --b2b-api JAR \\" >&2
  echo "  --frontend-ssr DIRECTORY --b2b-frontend DIRECTORY" >&2
  exit 2
}

release=''
contract_version=''
output=''
declare -A source=()
while (($#)); do
  case "$1" in
    --release) release="${2:-}"; shift 2 ;;
    --contract-version) contract_version="${2:-}"; shift 2 ;;
    --output) output="${2:-}"; shift 2 ;;
    --sbadmin|--api|--front-api|--ui|--b2b-api|--frontend-ssr|--b2b-frontend)
      source["${1#--}"]="${2:-}"; shift 2 ;;
    *) usage ;;
  esac
done

[[ "$release" =~ ^[0-9a-f]{7,64}$ ]] || { echo 'release must be a Git SHA' >&2; exit 2; }
[[ "$contract_version" =~ ^[A-Za-z0-9._-]+$ ]] || { echo 'contract version is invalid' >&2; exit 2; }
[[ -n "$output" && ! -e "$output" ]] || { echo 'output must not already exist' >&2; exit 2; }

artifacts=(sbadmin api front-api ui b2b-api frontend-ssr b2b-frontend)
for artifact in "${artifacts[@]}"; do
  [[ -n "${source[$artifact]:-}" && -e "${source[$artifact]}" ]] || {
    echo "missing source for ${artifact}" >&2; exit 2;
  }
done

mkdir -p "$output"
for artifact in sbadmin api front-api ui b2b-api; do
  cp -- "${source[$artifact]}" "$output/${artifact}.jar"
done
tar -C "${source[frontend-ssr]}" -czf "$output/frontend-ssr.tar.gz" .
tar -C "${source[b2b-frontend]}" -czf "$output/b2b-frontend.tar.gz" .

{
  echo 'format=1'
  echo "release=${release}"
  echo "contract=${contract_version}"
  for artifact in "${artifacts[@]}"; do
    file="$output/${artifact}.jar"
    [[ "$artifact" == *frontend* ]] && file="$output/${artifact}.tar.gz"
    printf 'artifact %s %s\n' "$artifact" "$(sha256sum "$file" | awk '{print $1}')"
  done
} > "$output/release-manifest"

echo "release bundle ready: ${output}"
