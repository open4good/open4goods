#!/usr/bin/env node
/**
 * Budget guard for the client entry chunk (GOU-2698). Run after `pnpm build`:
 * reads the entry file from the server-side client manifest and fails when its
 * gzip size (KiB) exceeds the budget. Before GOU-2698 the entry embedded the whole
 * @mdi/js package (1 MB gzip).
 *
 * Usage: node scripts/check-entry-chunk-size.mjs [budgetKb]
 */
import { readFileSync } from 'node:fs'
import { gzipSync } from 'node:zlib'

const BUDGET_KB = Number(process.argv[2] ?? 250)
const manifest = readFileSync(
  new URL(
    '../.output/server/chunks/build/client.precomputed.mjs',
    import.meta.url
  ),
  'utf8'
)

const entryIndex = manifest.indexOf('isEntry:true')
const entryFile = manifest
  .slice(Math.max(0, entryIndex - 600), entryIndex + 200)
  .match(/file:"([^"]+\.js)"/g)
  ?.at(-1)
  ?.match(/file:"([^"]+)"/)?.[1]

if (entryIndex < 0 || !entryFile) {
  console.error('::error::Entry chunk not found in client.precomputed.mjs')
  process.exit(1)
}

const content = readFileSync(
  new URL(`../.output/public/_nuxt/${entryFile}`, import.meta.url)
)
const gzipKb = gzipSync(content, { level: 9 }).length / 1024
console.log(
  `Entry chunk ${entryFile}: ${(content.length / 1024).toFixed(0)} KB raw, ${gzipKb.toFixed(1)} KB gzip (budget ${BUDGET_KB} KB)`
)

if (gzipKb > BUDGET_KB) {
  console.error(`::error::Entry chunk exceeds ${BUDGET_KB} KB gzip`)
  process.exit(1)
}
