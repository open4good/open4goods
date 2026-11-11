/**
 * Heap probe preloaded into the SSR server (GOU-2093).
 *
 * Usage:
 *   HEAP_PROBE_FILE=/tmp/heap.ndjson node --expose-gc \
 *     --require ./scripts/diagnostics/heap-probe.cjs .output/server/index.mjs
 *
 * Every HEAP_PROBE_INTERVAL_MS (default 5000) it forces a full GC (when
 * --expose-gc is set) and appends one NDJSON line with process.memoryUsage().
 * `kill -USR2 <pid>` writes a heap snapshot next to HEAP_PROBE_FILE.
 */
const fs = require('node:fs')
const path = require('node:path')
const v8 = require('node:v8')

const file =
  process.env.HEAP_PROBE_FILE || `/tmp/heap-probe-${process.pid}.ndjson`
const intervalMs = Number(process.env.HEAP_PROBE_INTERVAL_MS || 5000)
const startedAt = Date.now()

const sample = () => {
  if (typeof global.gc === 'function') {
    global.gc()
  }
  const { rss, heapTotal, heapUsed, external, arrayBuffers } =
    process.memoryUsage()
  fs.appendFileSync(
    file,
    `${JSON.stringify({ t: Date.now() - startedAt, rss, heapTotal, heapUsed, external, arrayBuffers })}\n`
  )
}

setInterval(sample, intervalMs).unref()

process.on('SIGUSR2', () => {
  sample()
  const target = path.join(
    path.dirname(file),
    `heap-${process.pid}-${Date.now()}.heapsnapshot`
  )
  v8.writeHeapSnapshot(target)
  fs.appendFileSync(file, `${JSON.stringify({ snapshot: target })}\n`)
})
