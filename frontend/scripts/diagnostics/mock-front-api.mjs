#!/usr/bin/env node
/**
 * Local stand-in for front-api, used by the SSR heap load test (GOU-2093).
 *
 * Serves JSON captured from the public nudger.fr routes so product and
 * category pages render with realistic payloads. Captures stay out of Git
 * (see README.md). Unmocked calls answer 404 and are logged once.
 *
 * Usage: node scripts/diagnostics/mock-front-api.mjs <captureDir> [port]
 */
import http from 'node:http'
import { readFileSync, readdirSync } from 'node:fs'

const [dir, port = '4102'] = process.argv.slice(2)

if (!dir) {
  console.error('Usage: node mock-front-api.mjs <captureDir> [port]')
  process.exit(2)
}

const read = file => readFileSync(`${dir}/${file}`, 'utf-8')

// The Nuxt routes serialise the DTO Sets as {}: restore the arrays front-api sends.
const SET_KEYS = new Set([
  'aggregatedScores',
  'akaBrands',
  'akaModels',
  'excludedCauses',
  'exclusions',
  'hardTags',
  'icecatFeaturesIds',
  'icecatTaxonomyIds',
  'mpn',
  'participateInACV',
  'participateInScores',
  'sku',
  'sources',
  'tags',
])

const toDto = value => {
  if (Array.isArray(value)) {
    return value.map(toDto)
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, entry]) => [
        key,
        SET_KEYS.has(key) &&
        entry &&
        typeof entry === 'object' &&
        !Array.isArray(entry)
          ? Object.values(entry)
          : toDto(entry),
      ])
    )
  }
  return value
}

const categories = read('categories.json')
const navigation = read('nav.json')
const templates = readdirSync(dir)
  .filter(file => /^product-\d+\.json$/.test(file))
  .map(file => toDto(JSON.parse(read(file))))
const urlByGtin = new Map(
  read('product-urls.txt')
    .split('\n')
    .filter(Boolean)
    .map(url => [url.replace(/^.*\/(\d+)-.*$/, '$1'), url])
)
const unmocked = new Set()
let served = 0

http
  .createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost')
    const send = (body, status = 200) => {
      res.writeHead(status, { 'content-type': 'application/json' })
      res.end(body)
    }
    let match

    if (url.pathname === '/category') {
      return send(categories)
    }
    if (url.pathname === '/category/navigation') {
      return send(navigation)
    }
    if ((match = url.pathname.match(/^\/category\/([a-z-]+)$/))) {
      try {
        return send(read(`category-${match[1]}.json`))
      } catch {
        return send('{}', 404)
      }
    }
    if (url.pathname === '/commercial-events') {
      return send(read('commercial-events.json'))
    }
    if ((match = url.pathname.match(/^\/products\/(\d+)$/))) {
      // Every GTIN gets a real product body under its own slug, so each path
      // renders as a distinct page.
      const gtin = match[1]
      const template = templates[served++ % templates.length]
      const fullSlug = urlByGtin.get(gtin) ?? template.fullSlug
      return send(
        JSON.stringify({
          ...template,
          gtin: Number(gtin),
          fullSlug: fullSlug.replace(/^\//, ''),
          slug: fullSlug.split('/').pop(),
        })
      )
    }
    if (req.method === 'POST' && url.pathname === '/products') {
      const size = Number(url.searchParams.get('pageSize') ?? 15)
      const data = Array.from(
        { length: size },
        (_, i) => templates[(served + i) % templates.length]
      )
      served += size
      return send(
        JSON.stringify({
          page: { number: 0, size, totalElements: 900, totalPages: 60 },
          data,
        })
      )
    }

    const key = `${req.method} ${url.pathname.replace(/\d{6,}/g, ':gtin')}`
    if (!unmocked.has(key)) {
      unmocked.add(key)
      console.log('unmocked', key)
    }
    return send('{}', 404)
  })
  .listen(Number(port), '127.0.0.1')
