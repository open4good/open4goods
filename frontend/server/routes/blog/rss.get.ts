import { defineEventHandler, getRequestURL, setResponseHeader } from 'h3'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { listBlogDocs } from '../../utils/blog-content'

/**
 * Blog RSS feed, served from the `blog` Nuxt Content collection. Replaces the 410 stub this route
 * carried while the collection didn't exist yet.
 *
 * Link construction uses the current request's own origin (h3's getRequestURL, same source
 * TheArticles.vue already uses via useRequestURL for JSON-LD) rather than
 * BlogConfiguration/UiConfig's namings.base-urls: that value is externalized per-environment
 * config not present in this checkout (see config-contract-and-environments), and the request
 * origin is the correct source anyway now that Nuxt -- not the Java UI module -- owns the blog.
 */

const escapeXml = (value: string): string =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')

export default defineEventHandler(async event => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')
  setResponseHeader(event, 'Content-Type', 'application/rss+xml; charset=UTF-8')

  const origin = getRequestURL(event).origin
  const docs = await listBlogDocs(event)

  const items = docs
    .map(doc => {
      const slug = doc.path.split('/').filter(Boolean).pop() ?? doc.path
      const link = `${origin}/blog/${slug}`
      const pubDate = doc.date ? new Date(doc.date).toUTCString() : undefined
      const categories = doc.tags
        .map(tag => `      <category>${escapeXml(tag)}</category>`)
        .join('\n')

      return [
        '    <item>',
        `      <title>${escapeXml(doc.title)}</title>`,
        `      <link>${escapeXml(link)}</link>`,
        `      <guid isPermaLink="true">${escapeXml(link)}</guid>`,
        doc.description ? `      <description>${escapeXml(doc.description)}</description>` : undefined,
        pubDate ? `      <pubDate>${pubDate}</pubDate>` : undefined,
        doc.author ? `      <author>${escapeXml(doc.author)}</author>` : undefined,
        categories || undefined,
        '    </item>',
      ]
        .filter(Boolean)
        .join('\n')
    })
    .join('\n')

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<rss version="2.0">',
    '  <channel>',
    '    <title>Nudger - Blog</title>',
    `    <link>${escapeXml(`${origin}/blog`)}</link>`,
    '    <description>Nudger blog articles</description>',
    items,
    '  </channel>',
    '</rss>',
  ].join('\n')
})
