import { setDomainLanguageCacheHeaders } from '../utils/cache-headers'

export default defineEventHandler(async event => {
  const config = useRuntimeConfig(event)
  const upstreamUrl = `${config.apiUrl}${event.path.replace('/api', '')}`

  const res = await $fetch.raw<ArrayBuffer>(upstreamUrl, {
    responseType: 'arrayBuffer',
  })

  setDomainLanguageCacheHeaders(
    event,
    'public, max-age=604800, s-maxage=604800, stale-while-revalidate=86400'
  )
  setResponseHeader(
    event,
    'Content-Type',
    res.headers.get('content-type') ?? 'image/x-icon'
  )

  return Buffer.from(res._data as ArrayBuffer)
})
