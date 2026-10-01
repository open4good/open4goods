#!/usr/bin/env bash
# TOCTOU and authorization failure-path fixtures for the promotion entrypoints (GOU-137).
#
# systemd-runtime.test.sh proves the golden path and simple rollback. This file proves the two
# properties that matter for a remote-write entrypoint under a race or missing authorization:
#   1. a racing writer cannot interleave with the critical section held by flock;
#   2. state mutated between a check and its later use is rejected, not silently trusted.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
fixture="$(mktemp -d)"
cleanup() {
  chmod -R u+w -- "$fixture" 2>/dev/null || true
  rm -rf -- "$fixture"
}
trap cleanup EXIT

build_bundle() {
  local release="$1" output="$2"
  local src="$fixture/src-${release}"
  rm -rf -- "$src"
  mkdir -p "$src/frontend-ssr" "$src/b2b-frontend"
  for name in sbadmin api front-api ui b2b-api exposed-docs geocode; do
    printf '%s\n' "$name-$release" > "$src/${name}.jar"
  done
  printf '%s\n' "frontend-$release" > "$src/frontend-ssr/index.html"
  printf '%s\n' "b2b-$release" > "$src/b2b-frontend/index.html"
  "$ROOT/scripts/deploy/build-release-bundle.sh" --release "$release" --contract-version 1 --output "$output" \
    --sbadmin "$src/sbadmin.jar" --api "$src/api.jar" --front-api "$src/front-api.jar" \
    --ui "$src/ui.jar" --b2b-api "$src/b2b-api.jar" --exposed-docs "$src/exposed-docs.jar" \
    --geocode "$src/geocode.jar" --frontend-ssr "$src/frontend-ssr" \
    --b2b-frontend "$src/b2b-frontend"
}

mkdir -p "$fixture/bin"
printf '%s\n' '#!/usr/bin/env bash' 'echo "$*" >> "'"$fixture"'/systemctl.log"' 'exit 0' > "$fixture/bin/systemctl"
printf '%s\n' '#!/usr/bin/env bash' 'printf 200' > "$fixture/bin/curl"
chmod +x "$fixture/bin/systemctl" "$fixture/bin/curl"

# Test-only HMAC key (GOU-174): not a secret, just a fixture standing in for the real
# O4G_GATE_PROOF_HMAC_KEY that only the pipeline holds in a deployed environment.
TEST_HMAC_KEY_HEX="$(printf '4%.0s' $(seq 1 64))"

write_gate_proof() {
  local proof_file="$1" release="$2" manifest_path="$3"
  local digest generated_at seal
  digest="$(sha256sum "$manifest_path" | awk '{print $1}')"
  generated_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  seal="$(TEST_HMAC_KEY_HEX="$TEST_HMAC_KEY_HEX" python3 - "$release" "$digest" "$generated_at" <<PYEOF
import os, sys
sys.path.insert(0, "$ROOT/scripts/deploy")
import gate_proof_seal
release, digest, generated_at = sys.argv[1:4]
proof = {
    "ready": True,
    "candidateSha": release,
    "promotionTarget": "beta",
    "manifestDigest": digest,
    "generatedAt": generated_at,
}
key = bytes.fromhex(os.environ["TEST_HMAC_KEY_HEX"])
print(gate_proof_seal.compute_seal(key, proof))
PYEOF
  )"
  printf '{"ready": true, "candidateSha": "%s", "promotionTarget": "beta", "manifestDigest": "%s", "generatedAt": "%s", "seal": "%s"}\n' \
    "$release" "$digest" "$generated_at" "$seal" > "$proof_file"
}

##############################################################################
# 1. flock actually serializes a racing writer publishing the same new release.
##############################################################################
race_release='1111111'
build_bundle "$race_release" "$fixture/bundle-race"
mkdir -p "$fixture/race-bin" "$fixture/runtime-race"
write_gate_proof "$fixture/gate-proof-race.json" "$race_release" "$fixture/bundle-race/release-manifest"
critical_log="$fixture/critical.log"
: > "$critical_log"
# A slow `mkdir` fires only for the staging directory, i.e. only inside the flock-held critical
# section, so two concurrent publishers racing for the same release SHA overlap there if (and
# only if) the lock fails to serialize them.
cat > "$fixture/race-bin/mkdir" <<EOF
#!/usr/bin/env bash
for arg in "\$@"; do
  case "\${arg##*/}" in
    *.staging.*)
      echo "enter \$\$ \$(date +%s%N)" >> "$critical_log"
      sleep 0.3
      echo "exit \$\$ \$(date +%s%N)" >> "$critical_log"
      ;;
  esac
done
exec /usr/bin/mkdir "\$@"
EOF
chmod +x "$fixture/race-bin/mkdir"
cp "$fixture/bin/systemctl" "$fixture/bin/curl" "$fixture/race-bin/"

pids=()
for _ in 1 2; do
  PATH="$fixture/race-bin:$PATH" env -u BASH_ENV \
    O4G_SYSTEMCTL="$fixture/race-bin/systemctl" O4G_CURL="$fixture/race-bin/curl" O4G_GATE_PROOF_HMAC_KEY="$TEST_HMAC_KEY_HEX" \
    "$ROOT/scripts/deploy/publish-java-release.sh" \
    --release "$race_release" --bundle "$fixture/bundle-race" --service api \
    --health-url http://127.0.0.1/health --root "$fixture/runtime-race" \
    --promotion-target beta --gate-proof "$fixture/gate-proof-race.json" \
    >>"$fixture/race.out" 2>&1 &
  pids+=("$!")
done
race_failed=0
for pid in "${pids[@]}"; do
  wait "$pid" || race_failed=1
done
test "$race_failed" -eq 0 || { echo "expected both racing publishers to succeed" >&2; cat "$fixture/race.out" >&2; exit 1; }
test -f "$fixture/runtime-race/releases/${race_release}/release-manifest"
test "$(readlink "$fixture/runtime-race/services/api/current")" = "../../releases/${race_release}"

# Only the racer that wins the lock first should ever see a missing release and stage one; the
# other must wait for the lock, then observe the now-existing release and skip staging entirely.
# If flock failed to serialize the two `[[ -e "$release_dir" ]]` checks against the later `mv`,
# both racers would independently decide the release is missing and both would call mkdir here.
python3 - "$critical_log" <<'PY'
import sys

lines = [line.split() for line in open(sys.argv[1]) if line.strip()]
if len(lines) != 2:
    raise SystemExit(f"expected exactly one racer to stage the release, both staged it: {lines}")
kind_enter, pid_enter, _ = lines[0]
kind_exit, pid_exit, _ = lines[1]
if (kind_enter, kind_exit) != ("enter", "exit") or pid_enter != pid_exit:
    raise SystemExit(f"malformed critical section log: {lines}")
PY

##############################################################################
# 2. TOCTOU: a bundle mutated after the pre-lock check but before the staged copy is used must
#    not be published. This is the failure mode the fix in this change closes: verification now
#    also runs against the staged copy, immediately before the release is made immutable.
##############################################################################
toctou_release='2222222'
build_bundle "$toctou_release" "$fixture/bundle-toctou"
mkdir -p "$fixture/toctou-bin" "$fixture/runtime-toctou"
write_gate_proof "$fixture/gate-proof-toctou.json" "$toctou_release" "$fixture/bundle-toctou/release-manifest"
cp "$fixture/bin/systemctl" "$fixture/bin/curl" "$fixture/toctou-bin/"
# Swap the bundle's api.jar for unverified content the first time `cp` is asked to stage it,
# simulating a writer that mutates the bundle in the window between the pre-lock check and use.
cat > "$fixture/toctou-bin/cp" <<EOF
#!/usr/bin/env bash
for arg in "\$@"; do
  case "\$arg" in
    */bundle-toctou/api.jar)
      printf 'tampered-payload' > "\$arg"
      ;;
  esac
done
exec /usr/bin/cp "\$@"
EOF
chmod +x "$fixture/toctou-bin/cp"

if PATH="$fixture/toctou-bin:$PATH" env -u BASH_ENV \
  O4G_SYSTEMCTL="$fixture/toctou-bin/systemctl" O4G_CURL="$fixture/toctou-bin/curl" O4G_GATE_PROOF_HMAC_KEY="$TEST_HMAC_KEY_HEX" \
  "$ROOT/scripts/deploy/publish-java-release.sh" \
  --release "$toctou_release" --bundle "$fixture/bundle-toctou" --service api \
  --health-url http://127.0.0.1/health --root "$fixture/runtime-toctou" \
  --promotion-target beta --gate-proof "$fixture/gate-proof-toctou.json" \
  >"$fixture/toctou.out" 2>&1; then
  echo "expected a bundle mutated mid-publish to be rejected" >&2
  cat "$fixture/toctou.out" >&2
  exit 1
fi
grep -q 'staged Java artifact changed since it was verified' "$fixture/toctou.out"
test ! -e "$fixture/runtime-toctou/releases/${toctou_release}"
test ! -L "$fixture/runtime-toctou/services/api/current"

##############################################################################
# 3. Nuxt publish: flock also serializes concurrent activations of the same release/service.
##############################################################################
nuxt_release='3333333'
build_bundle "$nuxt_release" "$fixture/bundle-nuxt"
mkdir -p "$fixture/runtime-nuxt/releases"
write_gate_proof "$fixture/gate-proof-nuxt.json" "$nuxt_release" "$fixture/bundle-nuxt/release-manifest"
PATH="$fixture/bin:$PATH" env -u BASH_ENV \
  O4G_SYSTEMCTL="$fixture/bin/systemctl" O4G_CURL="$fixture/bin/curl" O4G_GATE_PROOF_HMAC_KEY="$TEST_HMAC_KEY_HEX" \
  "$ROOT/scripts/deploy/publish-java-release.sh" \
  --release "$nuxt_release" --bundle "$fixture/bundle-nuxt" --service api \
  --health-url http://127.0.0.1/health --root "$fixture/runtime-nuxt" \
  --promotion-target beta --gate-proof "$fixture/gate-proof-nuxt.json" >/dev/null
: > "$fixture/nuxt-systemctl.log"
cat > "$fixture/race-bin/systemctl" <<EOF
#!/usr/bin/env bash
echo "\$*" >> "$fixture/nuxt-systemctl.log"
sleep 0.3
exit 0
EOF
chmod +x "$fixture/race-bin/systemctl"
nuxt_pids=()
for _ in 1 2; do
  PATH="$fixture/race-bin:$PATH" env -u BASH_ENV \
    O4G_SYSTEMCTL="$fixture/race-bin/systemctl" O4G_CURL="$fixture/race-bin/curl" O4G_GATE_PROOF_HMAC_KEY="$TEST_HMAC_KEY_HEX" \
    "$ROOT/scripts/deploy/publish-nuxt-release.sh" \
    --release "$nuxt_release" --service frontend --health-url http://127.0.0.1/health \
    --root "$fixture/runtime-nuxt" \
    --promotion-target beta --gate-proof "$fixture/gate-proof-nuxt.json" \
    >>"$fixture/nuxt-race.out" 2>&1 &
  nuxt_pids+=("$!")
done
nuxt_race_failed=0
for pid in "${nuxt_pids[@]}"; do
  wait "$pid" || nuxt_race_failed=1
done
test "$nuxt_race_failed" -eq 0 || { echo "expected both racing Nuxt activations to succeed" >&2; cat "$fixture/nuxt-race.out" >&2; exit 1; }
test "$(readlink "$fixture/runtime-nuxt/services/frontend/current")" = "../../releases/${nuxt_release}/frontend-ssr"
test "$(wc -l < "$fixture/nuxt-systemctl.log")" -eq 2

##############################################################################
# 4. Authorization: install-systemd-runtime.sh must not enable/reload units when unit validation
#    fails, even though every environment file is present and correctly shaped.
##############################################################################
mkdir -p "$fixture/auth-environment" "$fixture/auth-unit-dir" "$fixture/auth-bin"
for service in sbadmin api front-api ui b2b-api frontend b2b-frontend; do
  : > "$fixture/auth-environment/${service}.env"
  chmod 600 "$fixture/auth-environment/${service}.env"
done
printf '%s\n' '#!/usr/bin/env bash' 'echo "$*" >> "'"$fixture"'/auth-systemctl.log"' 'exit 0' > "$fixture/auth-bin/systemctl"
chmod +x "$fixture/auth-bin/systemctl"

# Positive control: prove O4G_SYSTEMCTL/O4G_SYSTEMD_ANALYZE are actually wired to these stubs
# before relying on the absence of a log entry below. Without this control, a stub that silently
# failed to resolve (e.g. a typo'd env var name) would make the negative assertion pass vacuously,
# because no stub -- passing or failing -- would ever be invoked.
printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "$fixture/auth-bin/systemd-analyze-ok"
chmod +x "$fixture/auth-bin/systemd-analyze-ok"
PATH="$fixture/auth-bin:$PATH" env -u BASH_ENV \
  O4G_SYSTEMCTL="$fixture/auth-bin/systemctl" O4G_SYSTEMD_ANALYZE="$fixture/auth-bin/systemd-analyze-ok" \
  "$ROOT/scripts/deploy/install-systemd-runtime.sh" \
  --unit-dir "$fixture/auth-unit-dir" --environment-dir "$fixture/auth-environment"
grep -qx 'enable open4goods.target' "$fixture/auth-systemctl.log"
control_log_lines="$(wc -l < "$fixture/auth-systemctl.log")"

printf '%s\n' '#!/usr/bin/env bash' 'echo "unit validation rejected" >&2' 'exit 1' > "$fixture/auth-bin/systemd-analyze"
chmod +x "$fixture/auth-bin/systemd-analyze"
if PATH="$fixture/auth-bin:$PATH" env -u BASH_ENV \
  O4G_SYSTEMCTL="$fixture/auth-bin/systemctl" O4G_SYSTEMD_ANALYZE="$fixture/auth-bin/systemd-analyze" \
  "$ROOT/scripts/deploy/install-systemd-runtime.sh" \
  --unit-dir "$fixture/auth-unit-dir" --environment-dir "$fixture/auth-environment"; then
  echo "expected failed unit validation to block systemd install" >&2
  exit 1
fi
test "$(wc -l < "$fixture/auth-systemctl.log")" -eq "$control_log_lines"

##############################################################################
# 5. Authorization: the beta systemd bootstrap refuses to run without root, and without the
#    legacy launcher/config it needs to safely migrate a running beta service. Neither check
#    performs any write, so it is safe to exercise as a non-root user in CI.
##############################################################################
if [[ "$(id -u)" == '0' ]]; then
  echo "SKIP: root-only bootstrap authorization check cannot run as root" >&2
else
  if "$ROOT/scripts/deploy/bootstrap-beta-systemd-runtime.sh" \
    --release "$nuxt_release" --bundle "$fixture/bundle-nuxt" --gate-proof "$fixture/gate-proof-nuxt.json" \
    >"$fixture/bootstrap-non-root.out" 2>&1; then
    echo "expected non-root bootstrap invocation to be rejected" >&2
    exit 1
  fi
  grep -q 'must run as root' "$fixture/bootstrap-non-root.out"
fi

echo 'OK: TOCTOU and authorization failure paths for the promotion entrypoints'
