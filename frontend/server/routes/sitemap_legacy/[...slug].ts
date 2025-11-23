import { setHeader, setResponseStatus } from 'h3'

export default defineEventHandler(async (event) => {
  const method = event.node.req.method ?? 'GET'

  if (method !== 'GET' && method !== 'HEAD') {
    setResponseStatus(event, 405)
    setHeader(event, 'Allow', 'GET, HEAD')
    return ''
  }

  const runtimeConfig = useRuntimeConfig()
  const staticServer = runtimeConfig.public?.staticServer

  if (!staticServer) {
    throw createError({
      statusCode: 500,
      statusMessage: 'Static server base URL is not configured.',
    })
  }

  const slug = event.context.params?.slug
  const pathSuffix = Array.isArray(slug) ? slug.join('/') : slug ?? ''
  const upstreamUrl = new URL(`/sitemap/${pathSuffix}`, staticServer)

  const query = getQuery(event)
  const searchParams = new URLSearchParams()

  for (const [key, value] of Object.entries(query)) {
    if (Array.isArray(value)) {
      value.forEach((entry) => {
        if (entry !== undefined) {
          searchParams.append(key, String(entry))
        }
      })
    } else if (value !== undefined) {
      searchParams.append(key, String(value))
    }
  }

  if ([...searchParams].length > 0) {
    upstreamUrl.search = searchParams.toString()
  }

  const forwardHeaders = new Headers(event.node.req.headers as HeadersInit)
  forwardHeaders.set('accept-encoding', 'identity')
  forwardHeaders.delete('connection')
  forwardHeaders.delete('content-length')
  forwardHeaders.delete('transfer-encoding')

  let upstreamResponse: Response

  try {
    upstreamResponse = await fetch(upstreamUrl, {
      method,
      headers: forwardHeaders,
    })
  } catch (error) {
    throw createError({
      statusCode: 502,
      statusMessage: 'Failed to reach static server for sitemap proxy.',
      cause: error,
    })
  }

  setResponseStatus(event, upstreamResponse.status)

  const excludedResponseHeaders = new Set(['content-encoding', 'content-length', 'transfer-encoding', 'connection'])

  upstreamResponse.headers.forEach((value, key) => {
    if (!excludedResponseHeaders.has(key.toLowerCase())) {
      setHeader(event, key, value)
    }
  })

  if (method === 'HEAD') {
    return ''
  }

  return upstreamResponse.text()
})
