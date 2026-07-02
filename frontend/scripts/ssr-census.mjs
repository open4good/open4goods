#!/usr/bin/env node
/**
 * SSR census: fetches a rendered page and reports what actually shipped in the
 * HTML (not what the SPA hydrates into). Used to verify product-page SEO
 * surfaces (title, meta, headings, JSON-LD, section content, affiliate link
 * rel, hreflang) survive SSR/production builds.
 *
 * Usage: node scripts/ssr-census.mjs <url> [--no-rel-check]
 */

const url = process.argv[2]
const noRelCheck = process.argv.includes('--no-rel-check')

if (!url) {
  console.error('Usage: node scripts/ssr-census.mjs <url> [--no-rel-check]')
  process.exit(2)
}

function decodeEntities(str) {
  return str
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#x27;/gi, "'")
    .replace(/&#39;/g, "'")
    .replace(/&#x2f;/gi, '/')
    .replace(/&nbsp;/g, ' ')
    .replace(/&#x([0-9a-f]+);/gi, (_, hex) => String.fromCodePoint(parseInt(hex, 16)))
    .replace(/&#(\d+);/g, (_, dec) => String.fromCodePoint(parseInt(dec, 10)))
}

function stripTags(html) {
  return decodeEntities(html.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ')).trim()
}

function getAttr(tag, name) {
  const re = new RegExp(`${name}\\s*=\\s*("([^"]*)"|'([^']*)')`, 'i')
  const m = tag.match(re)
  if (!m) return undefined
  return decodeEntities(m[2] ?? m[3] ?? '')
}

function collectTypes(node, acc) {
  if (Array.isArray(node)) {
    node.forEach((n) => collectTypes(n, acc))
    return
  }
  if (node && typeof node === 'object') {
    if (node['@type']) {
      if (Array.isArray(node['@type'])) acc.push(...node['@type'])
      else acc.push(node['@type'])
    }
    for (const key of Object.keys(node)) {
      if (key === '@type') continue
      collectTypes(node[key], acc)
    }
  }
}

async function main() {
  const res = await fetch(url)
  const html = await res.text()

  let failed = false
  const fail = (msg) => {
    failed = true
    console.log(`FAIL: ${msg}`)
  }

  console.log(`=== SSR census: ${url} (HTTP ${res.status}) ===\n`)

  // 1. title + meta description
  const titleMatch = html.match(/<title[^>]*>([\s\S]*?)<\/title>/i)
  const title = titleMatch ? decodeEntities(titleMatch[1].trim()) : undefined
  console.log(`title: ${title ?? '(none)'}`)

  let description
  const metaTags = html.match(/<meta\b[^>]*>/gi) ?? []
  for (const tag of metaTags) {
    if (/name\s*=\s*["']description["']/i.test(tag)) {
      description = getAttr(tag, 'content')
      break
    }
  }
  console.log(`description: ${description ?? '(none)'}\n`)

  // 2. h1-h4
  console.log('--- headings ---')
  const headingRe = /<h([1-4])\b[^>]*>([\s\S]*?)<\/h\1>/gi
  let hm
  const headings = []
  while ((hm = headingRe.exec(html))) {
    const text = stripTags(hm[2])
    headings.push({ tag: `h${hm[1]}`, text })
    console.log(`h${hm[1]}: ${text}`)
  }
  if (!headings.some((h) => h.tag === 'h1')) fail('no <h1> found')
  console.log('')

  // 3. sections
  console.log('--- sections ---')
  const sectionOpenRe = /<section\b[^>]*\bid\s*=\s*("([^"]*)"|'([^']*)')[^>]*>/gi
  const sectionMatches = []
  let sm
  while ((sm = sectionOpenRe.exec(html))) {
    sectionMatches.push({ id: sm[2] ?? sm[3], index: sm.index, end: sm.index + sm[0].length })
  }
  const idCounts = {}
  for (const s of sectionMatches) idCounts[s.id] = (idCounts[s.id] ?? 0) + 1
  for (const [id, count] of Object.entries(idCounts)) {
    if (count > 1) fail(`duplicate id="${id}" appears ${count} times`)
  }
  sectionMatches.forEach((s, i) => {
    const next = sectionMatches[i + 1]
    const sliceEnd = next ? next.index : Math.min(s.end + 20000, html.length)
    const raw = html.slice(s.end, sliceEnd)
    const text = stripTags(raw)
    const hasContent = text.length > 40
    console.log(`section#${s.id}: hasContent=${hasContent} (${text.length} chars)`)
  })
  console.log('')

  // 4. JSON-LD
  console.log('--- json-ld ---')
  const ldRe = /<script\b[^>]*type\s*=\s*["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi
  let lm
  let ldCount = 0
  while ((lm = ldRe.exec(html))) {
    ldCount++
    try {
      const parsed = JSON.parse(lm[1])
      const types = []
      collectTypes(parsed, types)
      console.log(`block ${ldCount}: OK, types=[${types.join(', ')}]`)
    } catch (e) {
      fail(`json-ld block ${ldCount} failed to parse: ${e.message}`)
    }
  }
  if (ldCount === 0) console.log('(no json-ld blocks found)')
  console.log('')

  // 5. /contrib/ anchors
  console.log('--- /contrib/ anchors ---')
  const anchorRe = /<a\b[^>]*href\s*=\s*("([^"]*)"|'([^']*)')[^>]*>/gi
  let am
  let contribCount = 0
  while ((am = anchorRe.exec(html))) {
    const href = am[2] ?? am[3]
    if (!href || !href.includes('/contrib/')) continue
    contribCount++
    const tag = am[0]
    const rel = getAttr(tag, 'rel')
    const target = getAttr(tag, 'target')
    console.log(`href=${href} rel="${rel ?? ''}" target="${target ?? ''}"`)
    if (!noRelCheck && !(rel ?? '').includes('sponsored')) {
      fail(`/contrib/ anchor missing "sponsored" in rel: ${href}`)
    }
  }
  if (contribCount === 0) console.log('(none found)')
  console.log('')

  // 6. hreflang
  console.log('--- hreflang ---')
  const linkRe = /<link\b[^>]*>/gi
  let lkm
  let hreflangCount = 0
  while ((lkm = linkRe.exec(html))) {
    const tag = lkm[0]
    if (!/rel\s*=\s*["']alternate["']/i.test(tag)) continue
    const hreflang = getAttr(tag, 'hreflang')
    const href = getAttr(tag, 'href')
    if (!hreflang) continue
    hreflangCount++
    console.log(`hreflang=${hreflang} href=${href}`)
  }
  if (hreflangCount === 0) console.log('(none found)')

  console.log(`\n=== ${failed ? 'FAIL' : 'PASS'} ===`)
  process.exit(failed ? 1 : 0)
}

main().catch((err) => {
  console.error('census error:', err)
  process.exit(2)
})
