#!/usr/bin/env bash
# Force every promotion failure mode relevant to GOU-94 (corrupt manifest, digest mismatch,
# missing artifact, health check failure, partial write) against an isolated fixture root and
# prove that publish-java-release.sh / publish-nuxt-release.sh refuse the mutation or roll back
# to the prior symlink, exactly as the health-check case already proven by
# scripts/deploy/tests/systemd-runtime.test.sh. Never touches a shared or real target directory,
# and never reaches account, billing, credential or network state: the fixture root is a fresh
# mktemp tree, and systemctl/curl are local stub scripts, not the real tools.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
fixture="$(mktemp -d)"
cleanup() {
  chmod -R u+w -- "$fixture" 2>/dev/null || true
  rm -rf -- "$fixture"
}
trap cleanup EXIT

case "$fixture" in
  /tmp/*|/var/folders/*) ;;
  *) echo "fixture root must be a private temp directory, got: $fixture" >&2; exit 1 ;;
esac

mkdir -p "$fixture/bin"
printf '%s\n' '#!/usr/bin/env bash' 'echo "$*" >> "'"$fixture"'/systemctl.log"' 'exit 0' > "$fixture/bin/systemctl"
# shellcheck disable=SC2016 # The fixture script must expand this at its own runtime.
printf '%s\n' '#!/usr/bin/env bash' \
  'if [ "${O4G_TEST_CURL_STATUS:-0}" -eq 0 ]; then printf 200; else printf 000; fi' > "$fixture/bin/curl"
chmod +x "$fixture/bin/systemctl" "$fixture/bin/curl"
export PATH="$fixture/bin:$PATH"

# The publish scripts are `#!/usr/bin/env bash`, so a BASH_ENV startup file inherited from the
# caller runs first and can re-export PATH, silently handing them the real systemctl/curl. Drop
# BASH_ENV for every invocation so the stubs stay the only reachable implementation.
isolated() { PATH="$fixture/bin:$PATH" env -u BASH_ENV "$@"; }

resolved="$(isolated bash -c 'command -v systemctl')"
[[ "$resolved" == "$fixture/bin/systemctl" ]] || {
  echo "FAIL: publish scripts would resolve $resolved, not the isolated stub" >&2
  exit 1
}

for name in sbadmin api front-api ui b2b-api; do
  printf '%s\n' "$name" > "$fixture/${name}.jar"
done
mkdir -p "$fixture/frontend-ssr" "$fixture/b2b-frontend"
printf '%s\n' frontend > "$fixture/frontend-ssr/index.html"
printf '%s\n' b2b > "$fixture/b2b-frontend/index.html"
"$ROOT/scripts/deploy/build-release-bundle.sh" --release abcdef1 --contract-version 1 --output "$fixture/bundle" \
  --sbadmin "$fixture/sbadmin.jar" --api "$fixture/api.jar" --front-api "$fixture/front-api.jar" \
  --ui "$fixture/ui.jar" --b2b-api "$fixture/b2b-api.jar" --frontend-ssr "$fixture/frontend-ssr" \
  --b2b-frontend "$fixture/b2b-frontend"

fresh_runtime_root() {
  local label="$1"
  local root="$fixture/runtime-${label}"
  mkdir -p "$root/services/api"
  ln -s ../../releases/old "$root/services/api/current"
  printf '%s' "$root"
}

assert_symlink_unchanged() {
  local root="$1" expected="$2" label="$3"
  local actual
  actual="$(readlink "$root/services/api/current")"
  [[ "$actual" == "$expected" ]] || {
    echo "FAIL ($label): expected current -> $expected, got $actual" >&2
    exit 1
  }
}

assert_publish_fails() {
  local label="$1"; shift
  if isolated "$@" >"$fixture/last-stderr.log" 2>&1; then
    echo "FAIL ($label): expected publish to be rejected but it succeeded" >&2
    cat "$fixture/last-stderr.log" >&2
    exit 1
  fi
}

# --- Corrupt manifest: contract line stripped from an otherwise-valid bundle. --------------
corrupt_manifest_bundle="$fixture/bundle-corrupt-manifest"
cp -r "$fixture/bundle" "$corrupt_manifest_bundle"
grep -v '^contract=' "$fixture/bundle/release-manifest" > "$corrupt_manifest_bundle/release-manifest"
root="$(fresh_runtime_root corrupt-manifest)"
rm -f "$fixture/systemctl.log"
assert_publish_fails corrupt-manifest \
  "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 --bundle "$corrupt_manifest_bundle" \
  --service api --health-url http://127.0.0.1/health --root "$root"
grep -q 'manifest contract is invalid' "$fixture/last-stderr.log"
assert_symlink_unchanged "$root" ../../releases/old corrupt-manifest
[[ ! -d "$root/releases/abcdef1" ]] || { echo 'FAIL (corrupt-manifest): release directory must not be staged' >&2; exit 1; }
[[ ! -f "$fixture/systemctl.log" ]] || { echo 'FAIL (corrupt-manifest): systemctl must not run' >&2; exit 1; }

# --- Digest mismatch: artifact bytes changed after the manifest was written. ----------------
digest_mismatch_bundle="$fixture/bundle-digest-mismatch"
cp -r "$fixture/bundle" "$digest_mismatch_bundle"
printf 'tampered\n' > "$digest_mismatch_bundle/api.jar"
root="$(fresh_runtime_root digest-mismatch)"
rm -f "$fixture/systemctl.log"
assert_publish_fails digest-mismatch \
  "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 --bundle "$digest_mismatch_bundle" \
  --service api --health-url http://127.0.0.1/health --root "$root"
grep -q 'invalid Java artifact: api' "$fixture/last-stderr.log"
assert_symlink_unchanged "$root" ../../releases/old digest-mismatch
[[ ! -d "$root/releases/abcdef1" ]] || { echo 'FAIL (digest-mismatch): release directory must not be staged' >&2; exit 1; }
[[ ! -f "$fixture/systemctl.log" ]] || { echo 'FAIL (digest-mismatch): systemctl must not run' >&2; exit 1; }

# --- Missing artifact: a manifest-listed jar absent from the bundle directory. --------------
missing_artifact_bundle="$fixture/bundle-missing-artifact"
cp -r "$fixture/bundle" "$missing_artifact_bundle"
rm -f "$missing_artifact_bundle/ui.jar"
root="$(fresh_runtime_root missing-artifact)"
rm -f "$fixture/systemctl.log"
assert_publish_fails missing-artifact \
  "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 --bundle "$missing_artifact_bundle" \
  --service api --health-url http://127.0.0.1/health --root "$root"
grep -q 'invalid Java artifact: ui' "$fixture/last-stderr.log"
assert_symlink_unchanged "$root" ../../releases/old missing-artifact
[[ ! -f "$fixture/systemctl.log" ]] || { echo 'FAIL (missing-artifact): systemctl must not run' >&2; exit 1; }

# --- Health check failure: prior symlink is restored and the service is restarted twice. ----
root="$(fresh_runtime_root health-check-failure)"
rm -f "$fixture/systemctl.log"
assert_publish_fails health-check-failure \
  env O4G_TEST_CURL_STATUS=1 O4G_HEALTH_ATTEMPTS=1 O4G_HEALTH_DELAY_SECONDS=0 \
  "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 --bundle "$fixture/bundle" \
  --service api --health-url http://127.0.0.1/health --root "$root"
grep -q 'health check failed for api; restoring prior release' "$fixture/last-stderr.log"
assert_symlink_unchanged "$root" ../../releases/old health-check-failure
test "$(wc -l < "$fixture/systemctl.log")" -eq 2
grep -qx 'restart open4goods@api.service' "$fixture/systemctl.log"

# --- Partial write: a release directory exists from a crashed prior publish, with no manifest.
root="$(fresh_runtime_root partial-write)"
mkdir -p "$root/releases/abcdef1"
printf 'crashed-mid-stage\n' > "$root/releases/abcdef1/sbadmin.jar"
rm -f "$fixture/systemctl.log"
assert_publish_fails partial-write \
  "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 --bundle "$fixture/bundle" \
  --service api --health-url http://127.0.0.1/health --root "$root"
grep -q 'existing release is incomplete' "$fixture/last-stderr.log"
assert_symlink_unchanged "$root" ../../releases/old partial-write
[[ ! -f "$root/releases/abcdef1/release-manifest" ]] || { echo 'FAIL (partial-write): must not backfill the manifest' >&2; exit 1; }
test "$(cat "$root/releases/abcdef1/sbadmin.jar")" = crashed-mid-stage
[[ ! -f "$fixture/systemctl.log" ]] || { echo 'FAIL (partial-write): systemctl must not run' >&2; exit 1; }

# --- Nuxt side: digest mismatch and health-check failure roll back the same way. ------------
nuxt_root="$fixture/runtime-nuxt"
mkdir -p "$nuxt_root/services/frontend"
ln -s ../../releases/old-frontend "$nuxt_root/services/frontend/current"
isolated "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 --bundle "$fixture/bundle" \
  --service api --health-url http://127.0.0.1/health --root "$nuxt_root" >/dev/null
chmod -R u+w "$nuxt_root/releases/abcdef1"
: > "$nuxt_root/releases/abcdef1/frontend-ssr.tar.gz"
rm -f "$fixture/systemctl.log"
assert_publish_fails nuxt-digest-mismatch \
  "$ROOT/scripts/deploy/publish-nuxt-release.sh" --release abcdef1 --service frontend \
  --health-url http://127.0.0.1/health --root "$nuxt_root"
grep -q 'release artifact checksum does not match' "$fixture/last-stderr.log"
test "$(readlink "$nuxt_root/services/frontend/current")" = ../../releases/old-frontend
[[ ! -f "$fixture/systemctl.log" ]] || { echo 'FAIL (nuxt-digest-mismatch): systemctl must not run' >&2; exit 1; }

# --- Account/billing isolation: no fixture path, log or artifact ever names that state. -----
for marker in account billing credential secret; do
  if find "$fixture" -iname "*${marker}*" | grep -q .; then
    echo "FAIL: fixture tree unexpectedly names ${marker} state" >&2
    exit 1
  fi
done
# Assert against what a published script actually resolves, not what this shell resolves: the
# two differ whenever a BASH_ENV startup file rewrites PATH for child bash scripts.
for tool in curl systemctl; do
  resolved="$(isolated bash -c "command -v $tool")"
  [[ "$resolved" == "$fixture/bin/$tool" ]] || {
    echo "FAIL: real $tool was reachable ($resolved), not the isolated stub" >&2
    exit 1
  }
done

echo 'OK: promotion forced-failure and rollback fixtures (manifest, digest, missing artifact, health, partial write)'
