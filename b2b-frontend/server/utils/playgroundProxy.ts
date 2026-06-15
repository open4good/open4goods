const MAX_PROXY_PAYLOAD_BYTES = 3 * 1024 * 1024
const ROUTER_TIMEOUT_MS = 75000

/**
 * Validates serialized payload size to keep relay requests compatible with serverless limits.
 */
export function assertPlaygroundPayloadSize(payload: unknown) {
  const serialized = JSON.stringify(payload)
  const bytes = new TextEncoder().encode(serialized).byteLength
  if (bytes > MAX_PROXY_PAYLOAD_BYTES) {
    throw createError({ statusCode: 413, statusMessage: 'playground_payload_too_large' })
  }
}

/**
 * Relays requests to the Router and normalizes status messages for UI-level classification.
 */
export async function fetchRouterWithMappedErrors(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  event: any,
  path: string,
  apiKey: string,
  method: 'GET' | 'POST',
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  body?: Record<string, any>,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  query?: Record<string, any>
) {
  const config = useRuntimeConfig(event)
  const routerBaseUrl = resolvePlaygroundRouterBaseUrl(config)

  try {
    return await $fetch(path, {
      method,
      baseURL: routerBaseUrl,
      timeout: ROUTER_TIMEOUT_MS,
      headers: {
        Authorization: `Bearer ${apiKey.trim()}`
      },
      query,
      body
    })
  } catch (error: unknown) {
    throw toPlaygroundProxyError(error)
  }
}

/**
 * Relays multipart form-data requests to the Router while keeping API keys server-side.
 */
export async function fetchRouterMultipartWithMappedErrors(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  event: any,
  path: string,
  apiKey: string,
  formData: FormData
) {
  const config = useRuntimeConfig(event)
  const routerBaseUrl = resolvePlaygroundRouterBaseUrl(config)
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), ROUTER_TIMEOUT_MS)

  try {
    const response = await fetch(resolveRuntimeUrl(routerBaseUrl, path), {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey.trim()}`
      },
      body: formData,
      signal: controller.signal
    })

    if (!response.ok) {
      const payload = await readErrorPayload(response)
      throw createError({
        statusCode: response.status,
        statusMessage: payload.code || mapPlaygroundProxyError(response.status),
        message: payload.message || mapPlaygroundProxyError(response.status),
        data: {
          code: payload.code || mapPlaygroundProxyError(response.status),
          message: payload.message,
          requestId: response.headers.get('x-infera-request-id') || undefined,
          statusCode: response.status
        }
      })
    }

    const contentType = response.headers.get('content-type') || ''
    if (contentType.includes('application/json')) {
      return await response.json()
    }

    return await response.text()
  } catch (error: unknown) {
    if ((error as { statusCode?: number })?.statusCode) {
      throw error
    }

    throw toPlaygroundTransportError(error)
  } finally {
    clearTimeout(timeout)
  }
}

/**
 * Relays binary responses to the client as a data URL so the UI can preview them inline.
 */
export async function fetchRouterBinaryAsDataUrl(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  event: any,
  path: string,
  apiKey: string,
  body: Record<string, unknown>
) {
  const config = useRuntimeConfig(event)
  const routerBaseUrl = resolvePlaygroundRouterBaseUrl(config)
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), ROUTER_TIMEOUT_MS)

  try {
    const response = await fetch(resolveRuntimeUrl(routerBaseUrl, path), {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey.trim()}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body),
      signal: controller.signal
    })

    if (!response.ok) {
      const payload = await readErrorPayload(response)
      throw createError({
        statusCode: response.status,
        statusMessage: payload.code || mapPlaygroundProxyError(response.status),
        message: payload.message || mapPlaygroundProxyError(response.status),
        data: {
          code: payload.code || mapPlaygroundProxyError(response.status),
          message: payload.message,
          requestId: response.headers.get('x-infera-request-id') || undefined,
          statusCode: response.status
        }
      })
    }

    const contentType = response.headers.get('content-type') || 'application/octet-stream'
    const buffer = Buffer.from(await response.arrayBuffer())
    return {
      contentType,
      dataUrl: `data:${contentType};base64,${buffer.toString('base64')}`
    }
  } catch (error: unknown) {
    if ((error as { statusCode?: number })?.statusCode) {
      throw error
    }

    throw toPlaygroundTransportError(error)
  } finally {
    clearTimeout(timeout)
  }
}

function mapPlaygroundProxyError(statusCode: number): string {
  if (statusCode === 401) return 'playground_unauthorized'
  if (statusCode === 403) return 'playground_forbidden'
  if (statusCode === 404) return 'playground_not_found'
  if (statusCode === 408) return 'playground_timeout'
  if (statusCode === 413) return 'playground_payload_too_large'
  if (statusCode === 429) return 'playground_rate_limited'
  if (statusCode >= 500) return 'playground_upstream_unavailable'
  return 'playground_proxy_error'
}

function toPlaygroundProxyError(error: unknown) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const err = error as any
  const isAbort = err?.name === 'AbortError' || err?.cause?.name === 'AbortError'
  const statusCode = isAbort ? 408 : Number(err?.response?.status || err?.statusCode || 502)
  const routerPayload = err?.response?._data || err?.data
  const routerError = routerPayload?.error || routerPayload
  const transportCause = transportErrorCause(err)
  const code = typeof routerError?.code === 'string' && routerError.code
    ? routerError.code
    : transportCause?.code || mapPlaygroundProxyError(statusCode)
  const message = typeof routerError?.message === 'string' ? routerError.message : transportCause?.message
  const requestId = err?.response?.headers?.get?.('x-infera-request-id')

  return createError({
    statusCode,
    statusMessage: code,
    message: message || code,
    data: {
      code,
      message,
      requestId,
      statusCode,
      cause: transportCause?.cause
    }
  })
}

function toPlaygroundTransportError(error: unknown) {
  const cause = transportErrorCause(error)
  const statusCode = cause?.statusCode || 502
  const code = cause?.code || mapPlaygroundProxyError(statusCode)

  return createError({
    statusCode,
    statusMessage: code,
    message: cause?.message || code,
    data: {
      code,
      message: cause?.message,
      statusCode,
      cause: cause?.cause
    }
  })
}

function transportErrorCause(error: unknown): { statusCode: number, code: string, message: string, cause: string } | null {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const err = error as any
  const name = String(err?.name || err?.cause?.name || '')
  const causeCode = String(err?.cause?.code || err?.code || '')
  const causeMessage = String(err?.cause?.message || err?.message || '')

  if (name === 'AbortError') {
    return {
      statusCode: 408,
      code: 'playground_router_timeout',
      message: `Playground proxy timed out after ${ROUTER_TIMEOUT_MS}ms before the router returned a response.`,
      cause: 'abort'
    }
  }

  if (['ECONNREFUSED', 'ENOTFOUND', 'EAI_AGAIN', 'ECONNRESET', 'UND_ERR_CONNECT_TIMEOUT'].includes(causeCode)) {
    return {
      statusCode: 502,
      code: 'playground_router_unreachable',
      message: `Playground proxy could not reach the router (${causeCode}).`,
      cause: causeCode
    }
  }

  if (causeMessage) {
    return {
      statusCode: 502,
      code: 'playground_router_transport_error',
      message: causeMessage,
      cause: causeCode || name || 'transport_error'
    }
  }

  return null
}

async function readErrorPayload(response: Response): Promise<{ code?: string, message?: string }> {
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('application/json')) {
    return {}
  }

  try {
    const payload = await response.json()
    const error = payload?.error || payload
    return {
      code: typeof error?.code === 'string' ? error.code : undefined,
      message: typeof error?.message === 'string' ? error.message : undefined
    }
  } catch {
    return {}
  }
}

function resolvePlaygroundRouterBaseUrl(config: ReturnType<typeof useRuntimeConfig>) {
  return config.routerProxyTarget || config.public.routerBaseUrl
}
