#!/usr/bin/env bash
# Boots the built .output bundle from a directory outside frontend/ (no content/ next to it, as in
# the frontend-ssr container) and checks the blog server routes that depend on Nuxt Content.
# Usage: scripts/check-standalone-output.sh [port]   (run from frontend/ after `pnpm build`)
set -euo pipefail

PORT="${1:-4105}"
SLUG="a-quoi-sert-l-esg"
OUTPUT_DIR="$(pwd)/.output"

test -f "$OUTPUT_DIR/server/index.mjs" || { echo "::error::Run pnpm build first ($OUTPUT_DIR missing)"; exit 1; }

WORK_DIR="$(mktemp -d)"
cp -a "$OUTPUT_DIR" "$WORK_DIR/.output"
LOG="$WORK_DIR/server.log"

cleanup() {
  [ -n "${SERVER_PID:-}" ] && kill "$SERVER_PID" 2>/dev/null || true
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

(cd "$WORK_DIR" && test ! -e content && HOST=127.0.0.1 PORT="$PORT" NODE_ENV=production \
  exec node .output/server/index.mjs >"$LOG" 2>&1) &
SERVER_PID=$!

BASE="http://127.0.0.1:$PORT"
for _ in $(seq 1 60); do
  curl -fs -o /dev/null "$BASE/blog/rss" && break
  kill -0 "$SERVER_PID" 2>/dev/null || { cat "$LOG"; echo "::error::Server exited"; exit 1; }
  sleep 1
done

fail() {
  echo "::error::$1"
  echo "--- server log ---"
  cat "$LOG"
  exit 1
}

fetch() {
  curl -s -o "$WORK_DIR/body" -w '%{http_code}' -H 'Host: nudger.fr' "$BASE$1"
}

code=$(fetch "/api/blog/articles?pageNumber=0&pageSize=12")
[ "$code" = 200 ] || fail "/api/blog/articles returned $code"
count=$(node -e "const b=JSON.parse(require('fs').readFileSync(process.argv[1],'utf8'));console.log((b.data||[]).filter(p=>p.body).length)" "$WORK_DIR/body")
[ "$count" -gt 0 ] || fail "/api/blog/articles returned no article with a body"
echo "/api/blog/articles: 200, $count articles with a body"

code=$(fetch "/api/blog/articles/$SLUG")
[ "$code" = 200 ] || fail "/api/blog/articles/$SLUG returned $code"
grep -q '"body":"<' "$WORK_DIR/body" || fail "/api/blog/articles/$SLUG has no rendered body"
echo "/api/blog/articles/$SLUG: 200 with a rendered body"

code=$(fetch "/sitemap/blog-posts.xml")
[ "$code" = 200 ] || fail "/sitemap/blog-posts.xml returned $code"
locs=$(grep -o '<loc>[^<]*/blog/[^<]*</loc>' "$WORK_DIR/body" | wc -l)
[ "$locs" -gt 0 ] || fail "/sitemap/blog-posts.xml lists no blog article for nudger.fr"
grep -q "/blog/$SLUG</loc>" "$WORK_DIR/body" || fail "/sitemap/blog-posts.xml misses /blog/$SLUG"
echo "/sitemap/blog-posts.xml: 200, $locs blog URLs"
