import { readdirSync, readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

// Guards against the SSR heap exhaustion of GOU-2093: an `isr` route rule made
// the renderer keep every rendered payload in the in-memory storage, one entry
// per product path, until the Node process ran out of heap.

const FRONTEND_DIR = resolve(process.cwd())
const PAGES_DIR = join(FRONTEND_DIR, 'app/pages')

const listVueFiles = (dir: string): string[] =>
  readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const path = join(dir, entry.name)
    if (entry.isDirectory()) {
      return listVueFiles(path)
    }
    return entry.name.endsWith('.vue') ? [path] : []
  })

const CACHING_RULE = /\b(isr|swr|cache)\s*:/

const extractDefineRouteRules = (source: string): string[] =>
  Array.from(
    source.matchAll(/defineRouteRules\(\{([\s\S]*?)\}\)/g),
    match => match[1] ?? ''
  )

describe('SSR memory guards (GOU-2093)', () => {
  it('declares no isr, swr or cache rule in page route rules', () => {
    const offenders = listVueFiles(PAGES_DIR).filter(file =>
      extractDefineRouteRules(readFileSync(file, 'utf-8')).some(rules =>
        CACHING_RULE.test(rules)
      )
    )

    expect(offenders).toEqual([])
  })

  it('detects a caching rule in defineRouteRules', () => {
    const rules = extractDefineRouteRules(
      'defineRouteRules({\n  isr: 3600,\n})'
    )

    expect(rules.some(rule => CACHING_RULE.test(rule))).toBe(true)
  })

  it('bounds the Nitro cache storage with an LRU driver', () => {
    const config = readFileSync(join(FRONTEND_DIR, 'nuxt.config.ts'), 'utf-8')
    const storageBlock = config.match(/storage:\s*\{\s*cache:\s*\{([^}]*)\}/)

    expect(storageBlock?.[1]).toMatch(/driver:\s*'lru-cache'/)
    expect(storageBlock?.[1]).toMatch(/maxSize:/)
  })

  // GOU-2152: the default heap limit follows the visible RAM, so the SSR
  // process held 1.1-1.9 GB of RSS in production. See
  // scripts/diagnostics/README.md before changing the value.
  it('sets an explicit heap limit on the SSR container', () => {
    const compose = readFileSync(
      join(FRONTEND_DIR, '../docker-compose.yml'),
      'utf-8'
    )
    const service = compose.match(
      /\n {2}frontend-ssr:\n([\s\S]*?)(?=\n {0,2}\S)/
    )

    expect(service?.[1]).toMatch(/NODE_OPTIONS=--max-old-space-size=512\b/)
  })
})
