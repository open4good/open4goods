#!/usr/bin/env node
/**
 * SSR heap load test (GOU-2093, GOU-2152).
 *
 * Replays a mix of real routes against a production build (`.output`),
 * reports response times and the memory sampled by `heap-probe.cjs`, so a leak
 * shows up as a heap that keeps growing instead of reaching a plateau.
 *
 * 1. pnpm build
 * 2. HEAP_PROBE_FILE=/tmp/heap.ndjson PORT=4101 HOST=127.0.0.1 \
 *      node --expose-gc --max-old-space-size=2048 \
 *      --require ./scripts/diagnostics/heap-probe.cjs .output/server/index.mjs
 * 3. node scripts/diagnostics/ssr-heap-load.mjs --base http://127.0.0.1:4101 \
 *      --probe /tmp/heap.ndjson --requests 6000 --concurrency 8
 *
 * Options:
 *   --base <url>          server under test (127.0.0.1:4100-4109 on build hosts)
 *   --host <host>         Host header sent (default nudger.fr)
 *   --routes <file>       one path per line (default: built-in mix below)
 *   --requests <n>        total requests (default 3000)
 *   --concurrency <n>     parallel requests (default 8)
 *   --probe <file>        NDJSON written by heap-probe.cjs, summarised at the end
 *   --max-heap-mb <n>     exit 1 when the last heap after GC exceeds it
 *   --max-rss-mb <n>      exit 1 when the highest sampled RSS exceeds it
 *
 * Every request gets unique tracking parameters (utm_*, gclid, fbclid), as
 * crawlers and ad clicks do in production.
 */
import { readFileSync } from 'node:fs'
import { parseArgs } from 'node:util'

const { values } = parseArgs({
  options: {
    base: { type: 'string', default: 'http://127.0.0.1:4101' },
    host: { type: 'string', default: 'nudger.fr' },
    routes: { type: 'string' },
    requests: { type: 'string', default: '3000' },
    concurrency: { type: 'string', default: '8' },
    probe: { type: 'string' },
    'max-heap-mb': { type: 'string' },
    'max-rss-mb': { type: 'string' },
  },
})

const DEFAULT_ROUTES = [
  '/',
  '/televiseurs',
  '/lave-linge',
  '/refrigerateurs',
  '/smartphones',
  '/televiseurs/8806094935660-samsung-qe55q60d',
  '/lave-linge/3838782522305-gorenje-wnei84aps',
  '/smartphones/0194253401803-apple-iphone-13',
  '/rechercher?q=tv',
  '/rechercher?q=lave+linge+hublot',
  '/rechercher?q=iphone',
  '/blog',
  '/impact-score',
  '/categories',
  '/sitemap.xml',
]

const routes = values.routes
  ? readFileSync(values.routes, 'utf-8')
      .split('\n')
      .map(line => line.trim())
      .filter(line => line && !line.startsWith('#'))
  : DEFAULT_ROUTES

const total = Number(values.requests)
const concurrency = Number(values.concurrency)

const withTracking = (route, i) => {
  const separator = route.includes('?') ? '&' : '?'
  const params = [
    `utm_source=load${i % 7}`,
    `utm_campaign=c${i}`,
    i % 3 === 0 ? `gclid=G${i}x${Date.now()}` : `fbclid=F${i}`,
  ]
  return `${route}${separator}${params.join('&')}`
}

const statuses = new Map()
const durations = []
let sent = 0
let bytes = 0
const startedAt = Date.now()

const worker = async () => {
  while (sent < total) {
    const i = sent++
    const path = withTracking(routes[i % routes.length], i)
    const requestStartedAt = performance.now()
    try {
      const response = await fetch(`${values.base}${path}`, {
        headers: {
          host: values.host,
          'x-forwarded-host': values.host,
          'x-forwarded-proto': 'https',
          'user-agent': 'gou-2093-heap-load',
        },
        redirect: 'manual',
      })
      const body = await response.arrayBuffer()
      bytes += body.byteLength
      durations.push(performance.now() - requestStartedAt)
      statuses.set(response.status, (statuses.get(response.status) ?? 0) + 1)
    } catch (error) {
      const key = `error:${error.cause?.code ?? error.name}`
      statuses.set(key, (statuses.get(key) ?? 0) + 1)
    }
    if (i > 0 && i % 500 === 0) {
      console.log(
        `${i}/${total} requests, ${((Date.now() - startedAt) / 1000).toFixed(0)} s`
      )
    }
  }
}

await Promise.all(Array.from({ length: concurrency }, worker))

const seconds = (Date.now() - startedAt) / 1000
console.log(
  `done: ${total} requests in ${seconds.toFixed(0)} s, ${(bytes / 1048576).toFixed(0)} MB received`
)
console.log('statuses:', Object.fromEntries(statuses))

durations.sort((a, b) => a - b)
const percentile = p =>
  durations[Math.min(durations.length - 1, Math.floor(durations.length * p))]
if (durations.length) {
  console.log(
    `response time: p50=${percentile(0.5).toFixed(0)} ms p95=${percentile(0.95).toFixed(0)} ms p99=${percentile(0.99).toFixed(0)} ms max=${durations.at(-1).toFixed(0)} ms`
  )
}

if (values.probe) {
  // Let the probe take a post-load sample.
  await new Promise(resolve => setTimeout(resolve, 7000))
  const samples = readFileSync(values.probe, 'utf-8')
    .split('\n')
    .filter(Boolean)
    .map(line => JSON.parse(line))
    .filter(sample => sample.heapUsed !== undefined)
  const mb = n => (n / 1048576).toFixed(0)
  const step = Math.max(1, Math.floor(samples.length / 12))
  for (let i = 0; i < samples.length; i += step) {
    const s = samples[i]
    console.log(
      `t=${(s.t / 1000).toFixed(0)}s heapUsed(${s.phase ?? 'after-gc'})=${mb(s.heapUsed)} MB heapTotal=${mb(s.heapTotal)} MB external=${mb(s.external)} MB rss=${mb(s.rss)} MB`
    )
  }
  const last = samples.at(-1)
  const max = key => Math.max(...samples.map(s => s[key]))
  console.log(
    `max: rss=${mb(max('rss'))} MB heapTotal=${mb(max('heapTotal'))} MB heapUsed=${mb(max('heapUsed'))} MB external=${mb(max('external'))} MB arrayBuffers=${mb(max('arrayBuffers'))} MB`
  )
  if (last.limit) {
    console.log(`heap_size_limit: ${mb(last.limit)} MB`)
  }
  console.log(
    `last heapUsed (${last.phase ?? 'after-gc'}): ${mb(last.heapUsed)} MB`
  )
  if (
    values['max-heap-mb'] &&
    last.heapUsed / 1048576 > Number(values['max-heap-mb'])
  ) {
    console.error(`heap above ${values['max-heap-mb']} MB`)
    process.exitCode = 1
  }
  if (
    values['max-rss-mb'] &&
    max('rss') / 1048576 > Number(values['max-rss-mb'])
  ) {
    console.error(`RSS above ${values['max-rss-mb']} MB`)
    process.exitCode = 1
  }
}
