#!/usr/bin/env bash
# GOU-164: publish-java-release.sh / publish-nuxt-release.sh must refuse to mutate systemd state
# (symlink swap, systemctl restart) without a fresh scripts/verify/check_promotion_readiness.py
# proof that names this exact candidate SHA, promotion target and manifest digest. Proves:
#   1. publish refused with no gate proof at all;
#   2. publish refused with a proof for a different candidate SHA (not replayable across SHAs);
#   3. publish refused with a proof for a different promotion target (not replayable across targets);
#   4. publish refused with a stale proof (bounded freshness, not an unbounded pass);
#   5. publish accepted with a fresh, exactly-matching proof -- the golden path these fixtures
#      guard against regressing;
#   6. GOU-174: publish refused with a hand-written proof naming every field correctly but
#      carrying no valid seal -- the exact counter-example from that issue's body, which passed
#      before this fix.
#   7. GOU-177: publish refused when the proof is signed with an unrelated Ed25519 key -- the
#      deploy host, holding only the pipeline's public key, cannot be fooled by a proof forged
#      with any other keypair.
# Never touches a shared or real target directory: the fixture root is a fresh mktemp tree and
# systemctl/curl are local stub scripts, not the real tools.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
fixture="$(mktemp -d)"
cleanup() {
  chmod -R u+w -- "$fixture" 2>/dev/null || true
  rm -rf -- "$fixture"
}
trap cleanup EXIT

mkdir -p "$fixture/bin"
printf '%s\n' '#!/usr/bin/env bash' 'echo "$*" >> "'"$fixture"'/systemctl.log"' 'exit 0' > "$fixture/bin/systemctl"
printf '%s\n' '#!/usr/bin/env bash' 'printf 200' > "$fixture/bin/curl"
chmod +x "$fixture/bin/systemctl" "$fixture/bin/curl"
# Test-only Ed25519 keypair (GOU-174/GOU-177): not the pipeline's real key, just a fixture
# standing in for it. The deploy host (isolated()) only ever gets TEST_PUBLIC_KEY_HEX.
TEST_SIGNING_KEY_HEX="$(printf '4%.0s' $(seq 1 64))"
TEST_PUBLIC_KEY_HEX="$(python3 -c '
import sys
sys.path.insert(0, "'"$ROOT"'/scripts/deploy")
import gate_proof_seal
from cryptography.hazmat.primitives import serialization
key = gate_proof_seal.load_signing_key_from_hex("'"$TEST_SIGNING_KEY_HEX"'")
print(key.public_key().public_bytes(
    encoding=serialization.Encoding.Raw, format=serialization.PublicFormat.Raw).hex())
')"
OTHER_SIGNING_KEY_HEX="$(printf '5%.0s' $(seq 1 64))"
isolated() {
  PATH="$fixture/bin:$PATH" env -u BASH_ENV O4G_SYSTEMCTL="$fixture/bin/systemctl" O4G_CURL="$fixture/bin/curl" \
    O4G_GATE_PROOF_PUBLIC_KEY="$TEST_PUBLIC_KEY_HEX" "$@"
}

# Derive the Java service list from publish-java-release.sh itself rather than recopying it,
# so this fixture cannot silently fall behind when a service is added there.
eval "$(grep -m1 '^readonly JAVA_SERVICES=' "$ROOT/scripts/deploy/publish-java-release.sh")"

for name in "${JAVA_SERVICES[@]}"; do
  printf '%s\n' "$name" > "$fixture/${name}.jar"
done
mkdir -p "$fixture/frontend-ssr" "$fixture/b2b-frontend"
printf '%s\n' frontend > "$fixture/frontend-ssr/index.html"
printf '%s\n' b2b > "$fixture/b2b-frontend/index.html"
release='abcdef1'
build_bundle_args=(--release "$release" --contract-version 1 --output "$fixture/bundle")
for name in "${JAVA_SERVICES[@]}"; do
  build_bundle_args+=("--${name}" "$fixture/${name}.jar")
done
build_bundle_args+=(--frontend-ssr "$fixture/frontend-ssr" --b2b-frontend "$fixture/b2b-frontend")
"$ROOT/scripts/deploy/build-release-bundle.sh" "${build_bundle_args[@]}"
manifest_digest="$(sha256sum "$fixture/bundle/release-manifest" | awk '{print $1}')"

# Signs the proof the way scripts/verify/check_promotion_readiness.py does, over exactly the
# fields written below, using a signing key -- by default the one whose public half `isolated`
# gives the verifier.
seal_for() {
  local ready="$1" sha="$2" target="$3" digest="$4" generated_at="$5"
  local signing_key_hex="${6:-$TEST_SIGNING_KEY_HEX}"
  SIGNING_KEY_HEX="$signing_key_hex" python3 - "$ready" "$sha" "$target" "$digest" "$generated_at" \
    <<PYEOF
import os, sys
sys.path.insert(0, "$ROOT/scripts/deploy")
import gate_proof_seal
ready, sha, target, digest, generated_at = sys.argv[1:6]
proof = {
    "ready": ready == "true",
    "candidateSha": sha,
    "promotionTarget": target,
    "manifestDigest": digest,
    "generatedAt": generated_at,
}
signing_key = gate_proof_seal.load_signing_key_from_hex(os.environ["SIGNING_KEY_HEX"])
print(gate_proof_seal.compute_seal(signing_key, proof))
PYEOF
}

write_proof() {
  local proof_file="$1"
  local release_field="$2"
  local target_field="$3"
  local digest_field="$4"
  local generated_at="$5"
  local ready="${6:-true}"
  local signing_key_hex="${7:-$TEST_SIGNING_KEY_HEX}"
  local seal
  seal="$(seal_for "$ready" "$release_field" "$target_field" "$digest_field" "$generated_at" "$signing_key_hex")"
  printf '{"ready": %s, "candidateSha": "%s", "promotionTarget": "%s", "manifestDigest": "%s", "generatedAt": "%s", "seal": "%s"}\n' \
    "$ready" "$release_field" "$target_field" "$digest_field" "$generated_at" "$seal" > "$proof_file"
}
now_iso() { date -u +%Y-%m-%dT%H:%M:%SZ; }
iso_minus_seconds() { date -u -d "@$(( $(date -u +%s) - "$1" ))" +%Y-%m-%dT%H:%M:%SZ; }

fresh_runtime_root() {
  local label="$1"
  local root="$fixture/runtime-${label}"
  mkdir -p "$root/services/api"
  ln -s ../../releases/old "$root/services/api/current"
  printf '%s' "$root"
}

assert_rejected() {
  local label="$1"
  local expected_message="$2"
  shift 2
  local root
  root="$(fresh_runtime_root "$label")"
  rm -f "$fixture/systemctl.log"
  if isolated "$ROOT/scripts/deploy/publish-java-release.sh" --release "$release" --bundle "$fixture/bundle" \
    --service api --health-url http://127.0.0.1/health --root "$root" "$@" \
    >"$fixture/last-out-${label}.log" 2>&1; then
    echo "FAIL ($label): expected publish to be rejected but it succeeded" >&2
    cat "$fixture/last-out-${label}.log" >&2
    exit 1
  fi
  grep -q "$expected_message" "$fixture/last-out-${label}.log" || {
    echo "FAIL ($label): expected stderr to mention '$expected_message'" >&2
    cat "$fixture/last-out-${label}.log" >&2
    exit 1
  }
  [[ "$(readlink "$root/services/api/current")" == ../../releases/old ]] || {
    echo "FAIL ($label): current symlink must not have been mutated" >&2
    exit 1
  }
  [[ ! -f "$fixture/systemctl.log" ]] || {
    echo "FAIL ($label): systemctl must not run when the gate proof is rejected" >&2
    exit 1
  }
}

##############################################################################
# 1. No --gate-proof flag at all: rejected before any mutation, as a usage error.
##############################################################################
root="$(fresh_runtime_root no-flag)"
rm -f "$fixture/systemctl.log"
if isolated "$ROOT/scripts/deploy/publish-java-release.sh" --release "$release" --bundle "$fixture/bundle" \
  --service api --health-url http://127.0.0.1/health --root "$root" --promotion-target beta \
  >"$fixture/no-flag.log" 2>&1; then
  echo 'FAIL (no-flag): expected publish without --gate-proof to be rejected' >&2
  cat "$fixture/no-flag.log" >&2
  exit 1
fi
grep -q 'gate proof path is required' "$fixture/no-flag.log"
[[ "$(readlink "$root/services/api/current")" == ../../releases/old ]]
[[ ! -f "$fixture/systemctl.log" ]]

##############################################################################
# 2. --gate-proof points at a file that does not exist: rejected, not silently skipped.
##############################################################################
assert_rejected missing-proof-file 'gate proof not found' \
  --promotion-target beta --gate-proof "$fixture/does-not-exist.json"

##############################################################################
# 3. Proof names a different candidate SHA: not replayable across SHAs.
##############################################################################
write_proof "$fixture/proof-wrong-sha.json" '1111111deadbeef' beta "$manifest_digest" "$(now_iso)"
assert_rejected wrong-sha 'gate proof does not name this exact candidate' \
  --promotion-target beta --gate-proof "$fixture/proof-wrong-sha.json"

##############################################################################
# 4. Proof names a different promotion target: not replayable across beta/production.
##############################################################################
write_proof "$fixture/proof-wrong-target.json" "$release" production "$manifest_digest" "$(now_iso)"
assert_rejected wrong-target 'gate proof does not name this exact candidate' \
  --promotion-target beta --gate-proof "$fixture/proof-wrong-target.json"

##############################################################################
# 5. Proof names a stale (edited/rebuilt) manifest digest: not replayable across manifests.
##############################################################################
write_proof "$fixture/proof-wrong-digest.json" "$release" beta '0000000000000000000000000000000000000000000000000000000000000000' "$(now_iso)"
assert_rejected wrong-digest 'gate proof does not name this exact candidate' \
  --promotion-target beta --gate-proof "$fixture/proof-wrong-digest.json"

##############################################################################
# 6. Proof is ready:false: rejected even though every other field matches (and is correctly
#    sealed for ready:false, so this fails on readiness, not on the seal).
##############################################################################
write_proof "$fixture/proof-not-ready.json" "$release" beta "$manifest_digest" "$(now_iso)" false
assert_rejected not-ready 'gate proof is not ready' \
  --promotion-target beta --gate-proof "$fixture/proof-not-ready.json"

##############################################################################
# 7. Stale proof: generated outside the bounded freshness window is rejected.
##############################################################################
write_proof "$fixture/proof-stale.json" "$release" beta "$manifest_digest" "$(iso_minus_seconds 1000)"
assert_rejected stale 'gate proof is stale' \
  --promotion-target beta --gate-proof "$fixture/proof-stale.json" --gate-proof-max-age-seconds 900

##############################################################################
# 8. GOU-174: the exact counter-example from the issue body. A hand-written proof naming the
#    real candidate SHA, target and manifest digest, fresh, but with no seal at all -- anyone who
#    can run the publish script could write this. Must now be rejected.
##############################################################################
printf '{"ready": true, "candidateSha": "%s", "promotionTarget": "beta", "manifestDigest": "%s", "generatedAt": "%s"}\n' \
  "$release" "$manifest_digest" "$(now_iso)" > "$fixture/proof-forged-no-seal.json"
assert_rejected forged-no-seal 'gate proof seal is missing or invalid' \
  --promotion-target beta --gate-proof "$fixture/proof-forged-no-seal.json"

##############################################################################
# 9. Same counter-example, but with a guessed/garbage seal value instead of none at all.
##############################################################################
printf '{"ready": true, "candidateSha": "%s", "promotionTarget": "beta", "manifestDigest": "%s", "generatedAt": "%s", "seal": "%s"}\n' \
  "$release" "$manifest_digest" "$(now_iso)" "$(printf 'd%.0s' $(seq 1 64))" > "$fixture/proof-forged-bad-seal.json"
assert_rejected forged-bad-seal 'gate proof seal is missing or invalid' \
  --promotion-target beta --gate-proof "$fixture/proof-forged-bad-seal.json"

##############################################################################
# 10. A correctly sealed, fresh, exactly-matching proof is rejected if the deploy host has no
#     gate proof public key configured (no O4G_GATE_PROOF_PUBLIC_KEY and no repo-committed
#     gate_proof_public_key.hex): the seal cannot be verified without it, so it must fail closed
#     rather than skip the check.
##############################################################################
write_proof "$fixture/proof-good-no-host-key.json" "$release" beta "$manifest_digest" "$(now_iso)"
root="$(fresh_runtime_root no-host-key)"
rm -f "$fixture/systemctl.log"
if PATH="$fixture/bin:$PATH" env -u BASH_ENV O4G_SYSTEMCTL="$fixture/bin/systemctl" O4G_CURL="$fixture/bin/curl" \
  "$ROOT/scripts/deploy/publish-java-release.sh" --release "$release" --bundle "$fixture/bundle" \
  --service api --health-url http://127.0.0.1/health --root "$root" \
  --promotion-target beta --gate-proof "$fixture/proof-good-no-host-key.json" \
  >"$fixture/no-host-key.log" 2>&1; then
  echo 'FAIL (no-host-key): expected publish to be rejected but it succeeded' >&2
  cat "$fixture/no-host-key.log" >&2
  exit 1
fi
grep -q 'no gate proof public key is provisioned' "$fixture/no-host-key.log" || {
  echo "FAIL (no-host-key): expected stderr to mention the missing key" >&2
  cat "$fixture/no-host-key.log" >&2
  exit 1
}
[[ "$(readlink "$root/services/api/current")" == ../../releases/old ]]
[[ ! -f "$fixture/systemctl.log" ]]

##############################################################################
# 11. GOU-177: a proof signed with an unrelated Ed25519 keypair -- the only forgery an attacker
#     holding just the deploy host's public key could even attempt -- is rejected. Unlike the
#     GOU-174 HMAC, no bytes this deploy host ever holds let it produce a proof that verifies.
##############################################################################
write_proof "$fixture/proof-wrong-keypair.json" "$release" beta "$manifest_digest" "$(now_iso)" \
  true "$OTHER_SIGNING_KEY_HEX"
assert_rejected wrong-keypair 'gate proof seal is missing or invalid' \
  --promotion-target beta --gate-proof "$fixture/proof-wrong-keypair.json"

##############################################################################
# 12. Golden path: a fresh, exactly-matching, correctly sealed proof is accepted and the
#     mutation proceeds, for both the Java and Nuxt publish entrypoints.
##############################################################################
write_proof "$fixture/proof-good.json" "$release" beta "$manifest_digest" "$(now_iso)"
root="$(fresh_runtime_root golden)"
rm -f "$fixture/systemctl.log"
isolated "$ROOT/scripts/deploy/publish-java-release.sh" --release "$release" --bundle "$fixture/bundle" \
  --service api --health-url http://127.0.0.1/health --root "$root" \
  --promotion-target beta --gate-proof "$fixture/proof-good.json" >"$fixture/golden-java.log"
[[ "$(readlink "$root/services/api/current")" == ../../releases/"${release}" ]]
grep -qx 'restart open4goods@api.service' "$fixture/systemctl.log"

mkdir -p "$root/services/frontend"
ln -s ../../releases/old-frontend "$root/services/frontend/current"
rm -f "$fixture/systemctl.log"
isolated "$ROOT/scripts/deploy/publish-nuxt-release.sh" --release "$release" --service frontend \
  --health-url http://127.0.0.1/health --root "$root" \
  --promotion-target beta --gate-proof "$fixture/proof-good.json" >"$fixture/golden-nuxt.log"
[[ "$(readlink "$root/services/frontend/current")" == ../../releases/"${release}"/frontend-ssr ]]
grep -qx 'restart open4goods-nuxt@frontend.service' "$fixture/systemctl.log"

echo 'OK: promotion-readiness gate proof authorization for the Java and Nuxt publish entrypoints'
