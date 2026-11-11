# SSR memory diagnostics

Tools used to reproduce and verify the SSR heap exhaustion of GOU-2093
(`JavaScript heap out of memory` on `frontend-ssr`, several times a day).

| File | Role |
| --- | --- |
| `heap-probe.cjs` | Preloaded into the server: forces a GC every few seconds and appends `process.memoryUsage()` to an NDJSON file; `kill -USR2 <pid>` writes a heap snapshot. |
| `mock-front-api.mjs` | Local stand-in for front-api serving captured public JSON, so product and category pages render real payloads. |
| `ssr-heap-load.mjs` | Replays a route mix with unique tracking parameters and summarises the heap after GC. |

Captures and route lists stay out of Git: keep them under `/tmp`.

## Reproduce

```bash
# 1. Capture a few public responses (moderate rate, read-only)
mkdir -p /tmp/ssr-capture && cd /tmp/ssr-capture
curl -s -o categories.json https://nudger.fr/api/categories
curl -s -o nav.json https://nudger.fr/api/categories/navigation
curl -s -o commercial-events.json https://nudger.fr/api/commercial-events
for id in tv oven smartphones dishwasher washing-machine refrigerator air-conditioner; do
  curl -s -o category-$id.json https://nudger.fr/api/categories/$id; sleep 0.3
done
curl -s https://nudger.fr/sitemap/fr/product-pages.xml \
  | grep -o '<loc>[^<]*</loc>' | sed 's#<loc>https://nudger.fr##;s#</loc>##' > product-urls.txt
for u in $(awk 'NR%1500==1' product-urls.txt | head -10); do
  g=$(echo "$u" | sed -E 's#.*/([0-9]+)-.*#\1#')
  curl -s -o product-$g.json "https://nudger.fr/api/products/$g"; sleep 0.5
done
shuf -n 1200 product-urls.txt > routes.txt   # add categories, /, /rechercher?q=... as needed

# 2. Build and start (ports 4100-4109 on build hosts)
cd frontend && pnpm build
node scripts/diagnostics/mock-front-api.mjs /tmp/ssr-capture 4102 &
PORT=4101 HOST=127.0.0.1 API_URL=http://127.0.0.1:4102 HEAP_PROBE_FILE=/tmp/heap.ndjson \
  node --expose-gc --max-old-space-size=2048 \
  --require ./scripts/diagnostics/heap-probe.cjs .output/server/index.mjs &

# 3. Load and read the heap trend
node scripts/diagnostics/ssr-heap-load.mjs --base http://127.0.0.1:4101 \
  --routes /tmp/ssr-capture/routes.txt --probe /tmp/heap.ndjson --requests 3000
```

## Findings (GOU-2093)

`isr: 3600` in `app/pages/[...slug].vue` and `app/pages/index.vue` caches no
HTML on the `node-server` preset, but it turns on payload extraction: the Nuxt
renderer stores the payload of every rendered path (150-300 KB) in the
`cache:nuxt:payload` storage, an unbounded in-memory Map, and never reads it
back (`experimental.payloadExtraction: false`). With about 16,000 product
paths per domain crawled by bots, the heap reached the 2 GB limit within hours.

Same scenario (3,000 requests, 1,200 distinct product paths):

| Build | Heap after GC under load | Heap after GC at rest |
| --- | --- | --- |
| Before (`isr: 3600`) | 431 → 763 MB, growing | 477 MB, of which 345 MB `cache:nuxt:payload` |
| After (no `isr`, LRU `cache` mount) | 370-420 MB, flat | 128 MB |

The `cache` storage mount is now an LRU (`nuxt.config.ts`, 1,000 entries,
128 MB) so cached handlers cannot fill the heap either.
`tests/ssr-memory-guards.spec.ts` fails if a page declares `isr`, `swr` or
`cache` route rules again.
