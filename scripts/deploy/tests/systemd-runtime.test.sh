#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
fixture="$(mktemp -d)"
cleanup() {
  chmod -R u+w -- "$fixture" 2>/dev/null || true
  rm -rf -- "$fixture"
}
trap cleanup EXIT

for name in sbadmin api front-api ui b2b-api exposed-docs geocode; do
  printf '%s\n' "$name" > "$fixture/${name}.jar"
done
mkdir -p "$fixture/frontend-ssr" "$fixture/b2b-frontend"
printf '%s\n' frontend > "$fixture/frontend-ssr/index.html"
printf '%s\n' b2b > "$fixture/b2b-frontend/index.html"
"$ROOT/scripts/deploy/build-release-bundle.sh" --release abcdef1 --contract-version 1 --output "$fixture/bundle" \
  --sbadmin "$fixture/sbadmin.jar" --api "$fixture/api.jar" --front-api "$fixture/front-api.jar" \
  --ui "$fixture/ui.jar" --b2b-api "$fixture/b2b-api.jar" \
  --exposed-docs "$fixture/exposed-docs.jar" --geocode "$fixture/geocode.jar" \
  --frontend-ssr "$fixture/frontend-ssr" \
  --b2b-frontend "$fixture/b2b-frontend"

mkdir -p "$fixture/bin" "$fixture/runtime/services/api"
printf '%s\n' '#!/usr/bin/env bash' 'echo "$*" >> "'"$fixture"'/systemctl.log"' 'exit 0' > "$fixture/bin/systemctl"
# shellcheck disable=SC2016 # The fixture script must expand this at its own runtime.
printf '%s\n' '#!/usr/bin/env bash' 'if [ "${O4G_TEST_CURL_STATUS:-0}" -eq 0 ]; then printf 200; else printf 000; fi' > "$fixture/bin/curl"
chmod +x "$fixture/bin/systemctl" "$fixture/bin/curl"
ln -s ../../releases/old "$fixture/runtime/services/api/current"

write_gate_proof() {
  local proof_file="$1" release="$2" manifest_path="$3"
  local digest generated_at
  digest="$(sha256sum "$manifest_path" | awk '{print $1}')"
  generated_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf '{"ready": true, "candidateSha": "%s", "promotionTarget": "beta", "manifestDigest": "%s", "generatedAt": "%s"}\n' \
    "$release" "$digest" "$generated_at" > "$proof_file"
}
write_gate_proof "$fixture/gate-proof.json" abcdef1 "$fixture/bundle/release-manifest"

# Stubs are wired in by absolute path (O4G_SYSTEMCTL/O4G_CURL/O4G_SYSTEMD_ANALYZE), the real
# guarantee that a test cannot reach a system binary. `isolated` also drops BASH_ENV as
# defense in depth: the deploy scripts are `#!/usr/bin/env bash`, so a BASH_ENV startup file
# inherited from the caller runs first and can re-export PATH, silently handing an unresolved
# call the real systemctl/curl.
isolated() { PATH="$fixture/bin:$PATH" env -u BASH_ENV "$@"; }

resolved="$(isolated bash -c 'command -v systemctl')"
[[ "$resolved" == "$fixture/bin/systemctl" ]] || {
  echo "FAIL: deploy scripts would resolve $resolved, not the isolated stub" >&2
  exit 1
}

systemctl_stub="$fixture/bin/systemctl"
curl_stub="$fixture/bin/curl"

isolated env O4G_SYSTEMCTL="$systemctl_stub" O4G_CURL="$curl_stub" \
  "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 --bundle "$fixture/bundle" \
  --service api --health-url http://127.0.0.1/health --promotion-target beta \
  --gate-proof "$fixture/gate-proof.json" --root "$fixture/runtime"
test "$(readlink "$fixture/runtime/services/api/current")" = ../../releases/abcdef1
grep -qx 'restart open4goods@api.service' "$fixture/systemctl.log"
test "$(wc -l < "$fixture/systemctl.log")" -eq 1
test ! -w "$fixture/runtime/releases/abcdef1/release-manifest"
test -f "$fixture/runtime/releases/abcdef1/frontend-ssr/index.html"

isolated env O4G_SYSTEMCTL="$systemctl_stub" O4G_CURL="$curl_stub" \
  "$ROOT/scripts/deploy/publish-nuxt-release.sh" --release abcdef1 \
  --service frontend --health-url http://127.0.0.1/health --promotion-target beta \
  --gate-proof "$fixture/gate-proof.json" --root "$fixture/runtime"
test "$(readlink "$fixture/runtime/services/frontend/current")" = ../../releases/abcdef1/frontend-ssr
grep -qx 'restart open4goods-nuxt@frontend.service' "$fixture/systemctl.log"

rm -f "$fixture/systemctl.log"
ln -s ../../releases/old "$fixture/runtime/services/api/current.rollback"
mv -Tf "$fixture/runtime/services/api/current.rollback" "$fixture/runtime/services/api/current"
if isolated env O4G_SYSTEMCTL="$systemctl_stub" O4G_CURL="$curl_stub" O4G_TEST_CURL_STATUS=1 O4G_HEALTH_ATTEMPTS=1 O4G_HEALTH_DELAY_SECONDS=0 \
  "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 \
  --bundle "$fixture/bundle" --service api --health-url http://127.0.0.1/health --promotion-target beta \
  --gate-proof "$fixture/gate-proof.json" --root "$fixture/runtime"; then
  echo 'expected failing health check' >&2
  exit 1
fi
test "$(readlink "$fixture/runtime/services/api/current")" = ../../releases/old
grep -qx 'restart open4goods@api.service' "$fixture/systemctl.log"
test "$(wc -l < "$fixture/systemctl.log")" -eq 2

ln -s ../../releases/old-frontend "$fixture/runtime/services/frontend/current.rollback"
mv -Tf "$fixture/runtime/services/frontend/current.rollback" "$fixture/runtime/services/frontend/current"
if isolated env O4G_SYSTEMCTL="$systemctl_stub" O4G_CURL="$curl_stub" O4G_TEST_CURL_STATUS=1 O4G_HEALTH_ATTEMPTS=1 O4G_HEALTH_DELAY_SECONDS=0 \
  "$ROOT/scripts/deploy/publish-nuxt-release.sh" --release abcdef1 --service frontend \
  --health-url http://127.0.0.1/health --promotion-target beta \
  --gate-proof "$fixture/gate-proof.json" --root "$fixture/runtime"; then
  echo 'expected failing Nuxt health check' >&2
  exit 1
fi
test "$(readlink "$fixture/runtime/services/frontend/current")" = ../../releases/old-frontend
grep -qx 'restart open4goods-nuxt@frontend.service' "$fixture/systemctl.log"

mkdir -p "$fixture/environment" "$fixture/unit-dir"
for service in sbadmin api front-api ui b2b-api exposed-docs geocode frontend b2b-frontend; do
  : > "$fixture/environment/${service}.env"
  chmod 600 "$fixture/environment/${service}.env"
done
printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "$fixture/bin/systemd-analyze"
printf '%s\n' '#!/usr/bin/env bash' 'echo "$*" >> "'"$fixture"'/systemctl.log"' 'exit 0' > "$fixture/bin/systemctl"
chmod +x "$fixture/bin/systemd-analyze" "$fixture/bin/systemctl"
systemd_analyze_stub="$fixture/bin/systemd-analyze"

isolated env O4G_SYSTEMCTL="$systemctl_stub" O4G_SYSTEMD_ANALYZE="$systemd_analyze_stub" \
  "$ROOT/scripts/deploy/install-systemd-runtime.sh" \
  --unit-dir "$fixture/unit-dir" --environment-dir "$fixture/environment"
test -f "$fixture/unit-dir/open4goods@.service"
test -f "$fixture/unit-dir/open4goods-nuxt@.service"
test -f "$fixture/unit-dir/opt-open4goods-.cached.mount"
grep -qx 'ReadWritePaths=/srv/open4goods /var/log/open4goods /opt/open4goods/.cached /opt/open4goods/backup' \
  "$fixture/unit-dir/open4goods@.service"
grep -qx 'RequiresMountsFor=/opt/open4goods/.cached' "$fixture/unit-dir/open4goods@.service"
grep -qx 'ConditionPathIsMountPoint=/opt/open4goods/.cached' "$fixture/unit-dir/open4goods@.service"
grep -qx 'ConditionPathIsMountPoint=/diskb' "$fixture/unit-dir/opt-open4goods-.cached.mount"
grep -qx 'enable open4goods.target' "$fixture/systemctl.log"
grep -qx 'enable opt-open4goods-.cached.mount' "$fixture/systemctl.log"

chmod 644 "$fixture/environment/api.env"
if isolated env O4G_SYSTEMCTL="$systemctl_stub" O4G_SYSTEMD_ANALYZE="$systemd_analyze_stub" \
  "$ROOT/scripts/deploy/install-systemd-runtime.sh" \
  --unit-dir "$fixture/unit-dir" --environment-dir "$fixture/environment"; then
  echo 'expected insecure environment file to be rejected' >&2
  exit 1
fi

systemd-analyze verify "$ROOT/ops/systemd/open4goods@.service" "$ROOT/ops/systemd/open4goods-nuxt@.service" \
  "$ROOT/ops/systemd/open4goods.target" "$ROOT/ops/systemd/opt-open4goods-.cached.mount"
echo 'OK: systemd release isolation, health rollback and unit syntax'
