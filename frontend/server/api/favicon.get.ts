import type { H3Event } from 'h3'
import { setDomainLanguageCacheHeaders } from '../utils/cache-headers'

const FAVICON_CACHE_CONTROL =
  'public, max-age=604800, s-maxage=604800, stale-while-revalidate=86400'

const PLACEHOLDER_FAVICON = `<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64" role="img" aria-label="Merchant">
  <rect width="64" height="64" rx="12" fill="#eef4fa"/>
  <path d="M16 28h32l-3-12H19l-3 12Z" fill="#1976d2"/>
  <path d="M20 28v22h24V28" fill="#ffffff"/>
  <path d="M24 34h16v16H24V34Z" fill="#c6ddf4"/>
  <path d="M16 28h32" stroke="#152e49" stroke-width="3" stroke-linecap="round"/>
</svg>`

const sendPlaceholder = (event: H3Event) => {
  setDomainLanguageCacheHeaders(event, FAVICON_CACHE_CONTROL)
  setResponseHeader(event, 'Content-Type', 'image/svg+xml; charset=utf-8')
  setResponseStatus(event, 200)
  return PLACEHOLDER_FAVICON
}

const isNotFoundResponse = (error: unknown) => {
  const response = (error as { response?: { status?: number } })?.response
  return response?.status === 404
}

export default defineEventHandler(async event => {
  const config = useRuntimeConfig(event)
  const query = getQuery(event)

  if (typeof query.url !== 'string' || query.url.trim().length === 0) {
    return sendPlaceholder(event)
  }

  const upstreamUrl = `${config.apiUrl}${event.path.replace('/api', '')}`

  try {
    const res = await $fetch.raw<ArrayBuffer>(upstreamUrl, {
      responseType: 'arrayBuffer',
    })

    setDomainLanguageCacheHeaders(event, FAVICON_CACHE_CONTROL)
    setResponseHeader(
      event,
      'Content-Type',
      res.headers.get('content-type') ?? 'image/x-icon'
    )

    return Buffer.from(res._data as ArrayBuffer)
  } catch (error) {
    if (!isNotFoundResponse(error)) {
      throw error
    }

    return sendPlaceholder(event)
  }
})
