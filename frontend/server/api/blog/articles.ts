import { getQuery } from 'h3'
import { detectLanguage } from '../../presentation/middleware/languageDetector'
import {
  applyCacheHeaders,
  CacheStrategies,
} from '../../presentation/middleware/cacheHeaders'
import {
  handleDomainError,
  handleUnknownError,
} from '../../presentation/middleware/errorHandler'
import {
  registerProviders,
  SERVICE_KEYS,
  getHandler,
} from '../../shared/di/providers'
import { isSuccess } from '../../shared/types/Result'
import type { GetArticlesHandler } from '../../application/blog/handlers/GetArticlesHandler'
import type { Page } from '../../domain/blog/entities/Page'
import type { Article } from '../../domain/blog/entities/Article'
import { DomainError } from '../../shared/errors'

/**
 * Blog articles API endpoint (Clean Architecture)
 * Orchestrates the use case execution
 */
export default defineEventHandler(async (event): Promise<Page<Article>> => {
  try {
    // 1. Detect language from request
    const domainLanguage = detectLanguage(event)

    // 2. Register dependencies
    registerProviders(domainLanguage)

    // 3. Apply cache headers
    applyCacheHeaders(event, CacheStrategies.ONE_HOUR)

    // 4. Parse query parameters
    const query = getQuery(event)
    const pageNumberParam = Array.isArray(query.pageNumber)
      ? query.pageNumber[0]
      : query.pageNumber
    const pageSizeParam = Array.isArray(query.pageSize)
      ? query.pageSize[0]
      : query.pageSize
    const tagParam = Array.isArray(query.tag) ? query.tag[0] : query.tag

    const pageNumber = pageNumberParam
      ? Number.parseInt(pageNumberParam, 10)
      : undefined
    const pageSize = pageSizeParam
      ? Number.parseInt(pageSizeParam, 10)
      : undefined
    const tag = typeof tagParam === 'string' ? tagParam : undefined

    // 5. Get handler from DI container
    const handler = getHandler<GetArticlesHandler>(
      SERVICE_KEYS.GET_ARTICLES_HANDLER
    )

    // 6. Execute use case
    const result = await handler.handle({
      pageNumber,
      pageSize,
      tag,
    })

    // 7. Handle result
    if (isSuccess(result)) {
      return result.value
    }

    // Error case
    handleDomainError(result.error, event)
  } catch (error) {
    if (error instanceof DomainError) {
      handleDomainError(error, event)
    }
    handleUnknownError(error, event)
  }

  // TypeScript requires a return, but handleDomainError/handleUnknownError throw
  throw createError({ statusCode: 500, statusMessage: 'Unexpected error' })
})
