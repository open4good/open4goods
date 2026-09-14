#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
fixture="$(mktemp -d)"
cleanup() {
  chmod -R u+w -- "$fixture" 2>/dev/null || true
  rm -rf -- "$fixture"
}
trap cleanup EXIT

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

mkdir -p "$fixture/bin" "$fixture/runtime/services/api"
printf '%s\n' '#!/usr/bin/env bash' 'echo "$*" >> "'"$fixture"'/systemctl.log"' 'exit 0' > "$fixture/bin/systemctl"
# shellcheck disable=SC2016 # The fixture script must expand this at its own runtime.
printf '%s\n' '#!/usr/bin/env bash' 'if [ "${O4G_TEST_CURL_STATUS:-0}" -eq 0 ]; then printf 200; else printf 000; fi' > "$fixture/bin/curl"
chmod +x "$fixture/bin/systemctl" "$fixture/bin/curl"
ln -s ../../releases/old "$fixture/runtime/services/api/current"

PATH="$fixture/bin:$PATH" "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 --bundle "$fixture/bundle" \
  --service api --health-url http://127.0.0.1/health --root "$fixture/runtime"
test "$(readlink "$fixture/runtime/services/api/current")" = ../../releases/abcdef1
grep -qx 'restart open4goods@api.service' "$fixture/systemctl.log"
test "$(wc -l < "$fixture/systemctl.log")" -eq 1
test ! -w "$fixture/runtime/releases/abcdef1/release-manifest"
test -f "$fixture/runtime/releases/abcdef1/frontend-ssr/index.html"

PATH="$fixture/bin:$PATH" "$ROOT/scripts/deploy/publish-nuxt-release.sh" --release abcdef1 \
  --service frontend --health-url http://127.0.0.1/health --root "$fixture/runtime"
test "$(readlink "$fixture/runtime/services/frontend/current")" = ../../releases/abcdef1/frontend-ssr
grep -qx 'restart open4goods-nuxt@frontend.service' "$fixture/systemctl.log"

rm -f "$fixture/systemctl.log"
ln -s ../../releases/old "$fixture/runtime/services/api/current.rollback"
mv -Tf "$fixture/runtime/services/api/current.rollback" "$fixture/runtime/services/api/current"
if PATH="$fixture/bin:$PATH" O4G_TEST_CURL_STATUS=1 O4G_HEALTH_ATTEMPTS=1 O4G_HEALTH_DELAY_SECONDS=0 \
  "$ROOT/scripts/deploy/publish-java-release.sh" --release abcdef1 \
  --bundle "$fixture/bundle" --service api --health-url http://127.0.0.1/health --root "$fixture/runtime"; then
  echo 'expected failing health check' >&2
  exit 1
fi
test "$(readlink "$fixture/runtime/services/api/current")" = ../../releases/old
grep -qx 'restart open4goods@api.service' "$fixture/systemctl.log"
test "$(wc -l < "$fixture/systemctl.log")" -eq 2

ln -s ../../releases/old-frontend "$fixture/runtime/services/frontend/current.rollback"
mv -Tf "$fixture/runtime/services/frontend/current.rollback" "$fixture/runtime/services/frontend/current"
if PATH="$fixture/bin:$PATH" O4G_TEST_CURL_STATUS=1 O4G_HEALTH_ATTEMPTS=1 O4G_HEALTH_DELAY_SECONDS=0 \
  "$ROOT/scripts/deploy/publish-nuxt-release.sh" --release abcdef1 --service frontend \
  --health-url http://127.0.0.1/health --root "$fixture/runtime"; then
  echo 'expected failing Nuxt health check' >&2
  exit 1
fi
test "$(readlink "$fixture/runtime/services/frontend/current")" = ../../releases/old-frontend
grep -qx 'restart open4goods-nuxt@frontend.service' "$fixture/systemctl.log"

mkdir -p "$fixture/environment" "$fixture/unit-dir"
for service in sbadmin api front-api ui b2b-api frontend b2b-frontend; do
  : > "$fixture/environment/${service}.env"
  chmod 600 "$fixture/environment/${service}.env"
done
printf '%s\n' '#!/usr/bin/env bash' 'exit 0' > "$fixture/bin/systemd-analyze"
printf '%s\n' '#!/usr/bin/env bash' 'echo "$*" >> "'"$fixture"'/systemctl.log"' 'exit 0' > "$fixture/bin/systemctl"
chmod +x "$fixture/bin/systemd-analyze" "$fixture/bin/systemctl"
PATH="$fixture/bin:$PATH" "$ROOT/scripts/deploy/install-systemd-runtime.sh" \
  --unit-dir "$fixture/unit-dir" --environment-dir "$fixture/environment"
test -f "$fixture/unit-dir/open4goods@.service"
test -f "$fixture/unit-dir/open4goods-nuxt@.service"
grep -qx 'enable open4goods.target' "$fixture/systemctl.log"

chmod 644 "$fixture/environment/api.env"
if PATH="$fixture/bin:$PATH" "$ROOT/scripts/deploy/install-systemd-runtime.sh" \
  --unit-dir "$fixture/unit-dir" --environment-dir "$fixture/environment"; then
  echo 'expected insecure environment file to be rejected' >&2
  exit 1
fi

systemd-analyze verify "$ROOT/ops/systemd/open4goods@.service" "$ROOT/ops/systemd/open4goods-nuxt@.service" \
  "$ROOT/ops/systemd/open4goods.target"
echo 'OK: systemd release isolation, health rollback and unit syntax'
