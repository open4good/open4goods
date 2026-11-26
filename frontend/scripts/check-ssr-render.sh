#!/usr/bin/env bash
# Boots the built .output bundle and renders `/` and a category page, to catch SSR ReferenceErrors
# (`xxx is not defined`: missing import, composable folder not auto-imported) that lint, vitest and
# the build all miss (GOU-2727, GOU-2728). The backend API is unreachable in CI: the check targets
# render errors, not data, so a 200 or a clean 404 passes on the category page.
# Usage: scripts/check-ssr-render.sh [port]   (run from frontend/ after `pnpm build`)
set -euo pipefail

PORT="${1:-4106}"
CATEGORY_PATH="/televiseurs/televiseur-55-pouces"
OUTPUT_DIR="$(pwd)/.output"

test -f "$OUTPUT_DIR/server/index.mjs" || { echo "::error::Run pnpm build first ($OUTPUT_DIR missing)"; exit 1; }

WORK_DIR="$(mktemp -d)"
LOG="$WORK_DIR/server.log"

cleanup() {
  [ -n "${SERVER_PID:-}" ] && kill "$SERVER_PID" 2>/dev/null || true
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

HOST=127.0.0.1 PORT="$PORT" NODE_ENV=production node "$OUTPUT_DIR/server/index.mjs" >"$LOG" 2>&1 &
SERVER_PID=$!

BASE="http://127.0.0.1:$PORT"
ready=0
for _ in $(seq 1 60); do
  # Any HTTP answer (even 500) means the server listens; the checks below judge the status.
  if curl -s -o /dev/null --max-time 5 "$BASE/robots.txt"; then ready=1; break; fi
  kill -0 "$SERVER_PID" 2>/dev/null || { cat "$LOG"; echo "::error::Server exited"; exit 1; }
  sleep 1
done
[ "$ready" = 1 ] || { cat "$LOG"; echo "::error::Server did not listen on $PORT"; exit 1; }

fail() {
  echo "::error::$1"
  echo "--- response (first 40 lines) ---"
  head -n 40 "$WORK_DIR/body" || true
  echo "--- server log (last 60 lines) ---"
  tail -n 60 "$LOG"
  exit 1
}

fetch() {
  curl -s -o "$WORK_DIR/body" --max-time 40 -w '%{http_code}' -H 'Host: nudger.fr' "$BASE$1" || echo 000
}

# Nuxt renders an error page as 500 (JSON or HTML); both carry the ReferenceError message.
code=$(fetch "/")
[ "$code" != 000 ] || fail "GET / got no answer"
[ "$code" -lt 500 ] || fail "GET / returned $code"
grep -q "is not defined" "$WORK_DIR/body" && fail "GET / body contains 'is not defined' (SSR ReferenceError)"
echo "GET /: $code, no SSR ReferenceError"

code=$(fetch "$CATEGORY_PATH")
[ "$code" != 000 ] || fail "GET $CATEGORY_PATH got no answer"
grep -q "is not defined" "$WORK_DIR/body" && fail "GET $CATEGORY_PATH body contains 'is not defined' (SSR ReferenceError)"
grep -q "is not defined" "$LOG" && fail "Server log contains 'is not defined' after rendering $CATEGORY_PATH"
echo "GET $CATEGORY_PATH: $code, no SSR ReferenceError"
