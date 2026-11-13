# SSR memory diagnostics

Tools used to reproduce and verify the SSR heap exhaustion of GOU-2093
(`JavaScript heap out of memory` on `frontend-ssr`, several times a day) and to
size the V8 heap limit of the SSR container (GOU-2152).

| File | Role |
| --- | --- |
| `heap-probe.cjs` | Preloaded into the server: appends `process.memoryUsage()` and the heap size limit to an NDJSON file every few seconds, after a forced GC (live heap) or, with `HEAP_PROBE_FORCE_GC=0`, as the process holds it (production view); `kill -USR2 <pid>` samples before and after a forced GC, then writes a heap snapshot unless `HEAP_PROBE_SNAPSHOT=0`. |
| `mock-front-api.mjs` | Local stand-in for front-api serving captured public JSON, so product and category pages render real payloads. |
| `ssr-heap-load.mjs` | Replays a route mix with unique tracking parameters and summarises response times (p50, p95, p99) and memory (`--max-heap-mb`, `--max-rss-mb` turn it into a check). |

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
# HEAP_PROBE_FORCE_GC=0 to see the RSS production sees, 1 (default) for the live heap
PORT=4101 HOST=127.0.0.1 API_URL=http://127.0.0.1:4102 HEAP_PROBE_FILE=/tmp/heap.ndjson \
  HEAP_PROBE_FORCE_GC=0 HEAP_PROBE_INTERVAL_MS=2000 \
  node --expose-gc --max-old-space-size=512 \
  --require ./scripts/diagnostics/heap-probe.cjs .output/server/index.mjs &

# 3. Load and read the memory trend and response times
node scripts/diagnostics/ssr-heap-load.mjs --base http://127.0.0.1:4101 \
  --routes /tmp/ssr-capture/routes.txt --probe /tmp/heap.ndjson --requests 3000 \
  --max-rss-mb 1000

# 4. Heap at rest: before and after a forced GC
kill -USR2 <server pid>
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

## Findings (GOU-2152): heap limit of the SSR container

After the GOU-2093 fix, production no longer ran out of heap, but the RSS of
`frontend-ssr` stayed between 1.1 and 1.6 GB, with a 1.87 GB peak. The
container started Node without `--max-old-space-size`, so V8 derived the heap
limit from the memory it saw and let the heap grow close to it before
collecting hard. Memory V8 has used is not given back to the OS quickly: the
RSS follows the highest heap reached, not the live heap.

Same scenario as GOU-2093 (3,000 requests, 1,200 distinct product paths,
`HEAP_PROBE_FORCE_GC=0`), one run at a time unless stated otherwise. Runs in
parallel share the single-threaded mock, so their response times only show
saturation of the bench, not of the server:

| Heap limit | Concurrency | RSS max | Heap total max | p95 | Heap after GC at rest | RSS at rest after GC | OOM |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1,536 MB (≈ production) | 8 | 1,369 MB | 1,112 MB | 1,260 ms | 124 MB | 1,159 MB | 0 |
| 512 MB | 8 | 691 MB | 506 MB | 1,268 ms | 122 MB | 628 MB | 0 |
| 512 MB, `MALLOC_ARENA_MAX=2` | 8 | 697 MB | 522 MB | 1,260 ms | 118 MB | 628 MB | 0 |
| 512 MB (two runs in parallel) | 16 | 758 MB | 550 MB | 2,757 ms | 119 MB | 672 MB | 0 |
| 512 MB (two runs in parallel, stopped after about 1,700 requests) | 32 | 785 MB | 599 MB | n/a | n/a | n/a | 0 |
| 640 MB (two runs in parallel) | 32 | 927 MB | 704 MB | 34,865 ms | n/a | n/a | 0 |

The live heap under load, sampled after a forced GC every 3 s at concurrency
8, peaks at 246 MB (1,536 MB limit): about 120 MB at rest plus 16 MB per page being rendered.
`external` and `arrayBuffers` stay under 31 MB: the footprint is V8 heap that
was not collected, not memory outside the heap, and limiting the number of
malloc arenas changes nothing. The Nitro `cache` LRU computes the size of its
entries (unstorage passes a `sizeCalculation` when `maxSize` is set), so it
stays bounded at 128 MB and needs no change.

**Value kept: `--max-old-space-size=512`.** It is twice the live heap at
concurrency 8 and held 1,700 requests at concurrency 32 without running out of heap, with a
heap used of 340-470 MB and an RSS near 700-800 MB, under the 1 GB target. At
concurrency 8 the p95 does not change (1,268 ms against 1,260 ms). 640 MB
brings the RSS to 927 MB at concurrency 32, too close to the target. The heap limit is not a cgroup
limit: if the live heap ever needs more than 512 MB, Node fails with `heap out
of memory` and the container restarts.

Where the value lives:

- `docker-compose.yml` (local stack): `NODE_OPTIONS=--max-old-space-size=512`
  on `frontend-ssr`, checked by `tests/ssr-memory-guards.spec.ts`.
- Beta and production: `docker-compose.frontend.yml` of `open4goods-config`
  (`frontend-ssr-blue`, `frontend-ssr-green`), copied by
  `publish-frontend.sh`. The same `NODE_OPTIONS` entry belongs there.

Check the limit a running container uses:

```bash
docker exec frontend-ssr-green node -p 'v8.getHeapStatistics().heap_size_limit / 1048576'
```

`docker exec` starts a new Node process with the container environment, so it
shows the limit `NODE_OPTIONS` gives, about 512 + 192 MB of young generation
(704 MB).
