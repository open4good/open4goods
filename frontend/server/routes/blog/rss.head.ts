import { defineEventHandler, setResponseHeader } from 'h3'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

/**
 * HEAD counterpart of rss.get.ts: same headers, no body.
 */
export default defineEventHandler(event => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')
  setResponseHeader(event, 'Content-Type', 'application/rss+xml; charset=UTF-8')
  return null
})
