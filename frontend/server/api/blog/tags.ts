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
import type { GetTagsHandler } from '../../application/blog/handlers/GetTagsHandler'
import type { Tag } from '../../domain/blog/entities/Tag'
import { DomainError } from '../../shared/errors'

/**
 * Blog tags API endpoint (Clean Architecture)
 */
export default defineEventHandler(async (event): Promise<Tag[]> => {
  try {
    // 1. Detect language
    const domainLanguage = detectLanguage(event)

    // 2. Register dependencies
    registerProviders(domainLanguage)

    // 3. Apply cache headers
    applyCacheHeaders(event, CacheStrategies.ONE_HOUR)

    // 4. Get handler from DI container
    const handler = getHandler<GetTagsHandler>(SERVICE_KEYS.GET_TAGS_HANDLER)

    // 5. Execute use case
    const result = await handler.handle({})

    // 6. Handle result
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
