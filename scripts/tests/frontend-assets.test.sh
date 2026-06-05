#!/usr/bin/env bash
set -euo pipefail

ROOT="$(pwd)"
FIXTURE_DIR="$(mktemp -d)"

mkdir -p "${FIXTURE_DIR}/staging/.output/public/_nuxt/builds"
mkdir -p "${FIXTURE_DIR}/current/.output/public/_nuxt/builds"
mkdir -p "${FIXTURE_DIR}/previous/.output/public/_nuxt"

printf 'current chunk\n' > "${FIXTURE_DIR}/staging/.output/public/_nuxt/current.js"
printf 'current latest\n' > "${FIXTURE_DIR}/staging/.output/public/_nuxt/builds/latest.json"
printf 'old chunk\n' > "${FIXTURE_DIR}/current/.output/public/_nuxt/old.js"
printf 'old latest\n' > "${FIXTURE_DIR}/current/.output/public/_nuxt/builds/latest.json"
printf 'older chunk\n' > "${FIXTURE_DIR}/previous/.output/public/_nuxt/older.css"

"${ROOT}/scripts/preserve-nuxt-assets.sh" \
  "${FIXTURE_DIR}/staging" \
  "${FIXTURE_DIR}/current" \
  "${FIXTURE_DIR}/previous"

[[ -f "${FIXTURE_DIR}/staging/.output/public/_nuxt/current.js" ]] || { echo "current chunk missing"; exit 1; }
[[ -f "${FIXTURE_DIR}/staging/.output/public/_nuxt/old.js" ]] || { echo "old chunk not preserved"; exit 1; }
[[ -f "${FIXTURE_DIR}/staging/.output/public/_nuxt/older.css" ]] || { echo "older chunk not preserved"; exit 1; }
if [[ "$(cat "${FIXTURE_DIR}/staging/.output/public/_nuxt/builds/latest.json")" != "current latest" ]]; then
  echo "latest.json was overwritten"
  exit 1
fi

mkdir -p "${FIXTURE_DIR}/web/_nuxt"
cat > "${FIXTURE_DIR}/web/index.html" <<'HTML'
<!doctype html>
<html>
  <head><link rel="stylesheet" href="/_nuxt/app.css"></head>
  <body><script type="module" src="/_nuxt/app.js"></script></body>
</html>
HTML
printf 'body{}\n' > "${FIXTURE_DIR}/web/_nuxt/app.css"
printf 'console.log("ok")\n' > "${FIXTURE_DIR}/web/_nuxt/app.js"

python3 -m http.server 8765 --directory "${FIXTURE_DIR}/web" >/tmp/open4goods-asset-test.log 2>&1 &
server_pid=$!
trap 'kill "${server_pid}" >/dev/null 2>&1 || true' EXIT
sleep 1

"${ROOT}/scripts/sweep-nuxt-assets.py" --base-url http://127.0.0.1:8765 --url / --json >/tmp/open4goods-asset-test-ok.json
grep -q '"classification": "ok"' /tmp/open4goods-asset-test-ok.json || { echo "asset sweep did not pass"; exit 1; }

rm "${FIXTURE_DIR}/web/_nuxt/app.js"
if "${ROOT}/scripts/sweep-nuxt-assets.py" --base-url http://127.0.0.1:8765 --url / --json >/tmp/open4goods-asset-test-fail.json; then
  echo "asset sweep should fail when an asset is missing"
  exit 1
fi
grep -q 'frontend_asset_failure' /tmp/open4goods-asset-test-fail.json || { echo "missing frontend_asset_failure classification"; exit 1; }

echo "frontend asset hardening tests passed"
