import {
  createError,
  defineEventHandler,
  getRequestIP,
  getRouterParam,
  setResponseHeader,
} from 'h3'

import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../../utils/log-backend-error'

/**
 * Per-user user→manufacturing distance. NOT cacheable. This is the ONLY route
 * that forwards the real client IP to the backend (via X-Forwarded-For), so the
 * backend GeoIP can resolve the user's location for the distance computation.
 */
export default defineEventHandler(async event => {
  setResponseHeader(event, 'Cache-Control', 'no-store')

  const gtinParam = getRouterParam(event, 'gtin')
  if (!gtinParam) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Product GTIN is required',
    })
  }
  const parsedGtin = Number.parseInt(gtinParam, 10)
  if (!Number.isFinite(parsedGtin)) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Product GTIN must be numeric',
    })
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const clientIp = getRequestIP(event, { xForwardedFor: true }) ?? ''

  const { apiUrl, machineToken } = useRuntimeConfig(event)

  try {
    return await $fetch(`${apiUrl}/brands/distance/${parsedGtin}`, {
      headers: {
        'X-Shared-Token': machineToken as string,
        ...(clientIp
          ? { 'X-Forwarded-For': clientIp, 'X-Real-Ip': clientIp }
          : {}),
      },
      query: { domainLanguage },
    })
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
