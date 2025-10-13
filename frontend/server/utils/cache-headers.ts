import type { H3Event } from 'h3'
import { setResponseHeader } from 'h3'

const HOST_AWARE_VARY_HEADER = 'Host, X-Forwarded-Host'

/**
 * Ensures CDN caches keep separate entries per hostname by varying on host headers.
 */
export const setDomainLanguageCacheHeaders = (
  event: H3Event,
  cacheControl: string
) => {
  setResponseHeader(event, 'Cache-Control', cacheControl)
  setResponseHeader(event, 'Vary', HOST_AWARE_VARY_HEADER)
}
