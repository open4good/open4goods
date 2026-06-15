import { createError, defineEventHandler, getQuery, getRouterParam } from 'h3'

import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'

/**
 * SSR-friendly proxy for the enriched brand payload (company, manufacturing
 * chain, scores, x-metas). Static per brand → public cache.
 */
export default defineEventHandler(async event => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  const brandName = getRouterParam(event, 'brandName')
  if (!brandName) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Brand name is required',
    })
  }

  const query = getQuery(event)
  const verticalId = query.verticalId ? String(query.verticalId) : undefined

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const { apiUrl, machineToken } = useRuntimeConfig(event)

  try {
    return await $fetch(`${apiUrl}/brands/${encodeURIComponent(brandName)}`, {
      headers: { 'X-Shared-Token': machineToken as string },
      query: {
        domainLanguage,
        ...(verticalId ? { verticalId } : {}),
      },
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
