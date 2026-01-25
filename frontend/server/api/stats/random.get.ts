import { cachedEventHandler } from 'nitropack/runtime/internal/cache'
import type { H3Event } from 'h3'
import type { ProductDto } from '~~/shared/api-client'
import { StatsApi } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { createBackendApiConfig } from '~~/shared/api-client/services/createBackendApiConfig'

type RandomStatsCacheContext = {
  domainLanguage: string
  num: number
}

declare module 'h3' {
  interface H3EventContext {
    randomStatsCacheContext?: RandomStatsCacheContext
  }
}

const resolveRandomStatsCacheContext = (
  event: H3Event
): RandomStatsCacheContext => {
  if (event.context.randomStatsCacheContext) {
    return event.context.randomStatsCacheContext
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const query = getQuery(event)
  const num = typeof query.num === 'string' ? parseInt(query.num, 10) : 1

  const context: RandomStatsCacheContext = { domainLanguage, num }

  event.context.randomStatsCacheContext = context

  return context
}

const handler = async (event: H3Event): Promise<ProductDto[]> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=60, s-maxage=60')

  const { domainLanguage, num } = resolveRandomStatsCacheContext(event)
  const statsApi = new StatsApi(createBackendApiConfig())

  try {
    const result = await statsApi.random({
      domainLanguage: domainLanguage as 'fr' | 'en',
      num,
      minOffersCount: 1,
    })

    // The API returns ProductDto but we expect an array
    return Array.isArray(result) ? result : [result]
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)

    console.error(
      'Error fetching random products:',
      backendError.logMessage,
      backendError
    )

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
}

export default cachedEventHandler(handler, {
  name: 'random-products',
  maxAge: 60,
  getKey: event => {
    const { domainLanguage, num } = resolveRandomStatsCacheContext(event)

    return `${domainLanguage}-${num}`
  },
})
