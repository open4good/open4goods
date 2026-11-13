/**
 * Heap probe preloaded into the SSR server (GOU-2093, GOU-2152).
 *
 * Usage:
 *   HEAP_PROBE_FILE=/tmp/heap.ndjson node --expose-gc \
 *     --require ./scripts/diagnostics/heap-probe.cjs .output/server/index.mjs
 *
 * Every HEAP_PROBE_INTERVAL_MS (default 5000) it appends one NDJSON line with
 * process.memoryUsage() and the V8 heap size limit. By default it forces a
 * full GC first (when --expose-gc is set), so the line shows the live heap;
 * HEAP_PROBE_FORCE_GC=0 samples without GC, so the line shows what the process
 * really holds, as in production.
 * `kill -USR2 <pid>` appends a sample before and after a forced GC, then writes
 * a heap snapshot next to HEAP_PROBE_FILE unless HEAP_PROBE_SNAPSHOT=0.
 */
const fs = require('node:fs')
const path = require('node:path')
const v8 = require('node:v8')

const file =
  process.env.HEAP_PROBE_FILE || `/tmp/heap-probe-${process.pid}.ndjson`
const intervalMs = Number(process.env.HEAP_PROBE_INTERVAL_MS || 5000)
const forceGc = process.env.HEAP_PROBE_FORCE_GC !== '0'
const snapshot = process.env.HEAP_PROBE_SNAPSHOT !== '0'
const startedAt = Date.now()

const gc = () => {
  if (typeof global.gc === 'function') {
    global.gc()
  }
}

const sample = (phase = forceGc ? 'after-gc' : 'live') => {
  const { rss, heapTotal, heapUsed, external, arrayBuffers } =
    process.memoryUsage()
  const limit = v8.getHeapStatistics().heap_size_limit
  fs.appendFileSync(
    file,
    `${JSON.stringify({ t: Date.now() - startedAt, phase, rss, heapTotal, heapUsed, external, arrayBuffers, limit })}\n`
  )
}

setInterval(() => {
  if (forceGc) {
    gc()
  }
  sample()
}, intervalMs).unref()

process.on('SIGUSR2', () => {
  sample('before-gc')
  gc()
  sample('after-gc')
  if (!snapshot) {
    return
  }
  const target = path.join(
    path.dirname(file),
    `heap-${process.pid}-${Date.now()}.heapsnapshot`
  )
  v8.writeHeapSnapshot(target)
  fs.appendFileSync(file, `${JSON.stringify({ snapshot: target })}\n`)
})
