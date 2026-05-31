import { defineEventHandler, setHeader } from 'h3'

const ROBOTS_TXT = [
  'User-agent: *',
  'Allow: /',
  'Disallow: /contrib',
  'Sitemap: https://nudger.fr/sitemap_index.xml',
  '',
].join('\n')

export default defineEventHandler(event => {
  setHeader(event, 'Content-Type', 'text/plain; charset=utf-8')

  return ROBOTS_TXT
})
