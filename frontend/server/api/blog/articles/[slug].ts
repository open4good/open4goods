import { detectLanguage } from '../../../presentation/middleware/languageDetector'
import {
  applyCacheHeaders,
  CacheStrategies,
} from '../../../presentation/middleware/cacheHeaders'
import {
  handleDomainError,
  handleUnknownError,
} from '../../../presentation/middleware/errorHandler'
import {
  registerProviders,
  SERVICE_KEYS,
  getHandler,
} from '../../../shared/di/providers'
import { isSuccess } from '../../../shared/types/Result'
import type { GetArticleBySlugHandler } from '../../../application/blog/handlers/GetArticleBySlugHandler'
import type { Article } from '../../../domain/blog/entities/Article'
import { DomainError } from '../../../shared/errors'

/**
 * Blog article by slug API endpoint (Clean Architecture)
 */
export default defineEventHandler(async (event): Promise<Article> => {
  try {
    // 1. Detect language
    const domainLanguage = detectLanguage(event)

    // 2. Register dependencies
    registerProviders(domainLanguage)

    // 3. Apply cache headers
    applyCacheHeaders(event, CacheStrategies.ONE_HOUR)

    // 4. Get slug from route params
    const slug = getRouterParam(event, 'slug')
    if (!slug) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Article slug is required',
      })
    }

    // 5. Get handler from DI container
    const handler = getHandler<GetArticleBySlugHandler>(
      SERVICE_KEYS.GET_ARTICLE_BY_SLUG_HANDLER
    )

    // 6. Execute use case
    const result = await handler.handle({ slug })

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

  throw createError({ statusCode: 500, statusMessage: 'Unexpected error' })
})
