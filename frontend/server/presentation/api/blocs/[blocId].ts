import { detectLanguage } from '../../middleware/languageDetector'
import {
  applyCacheHeaders,
  CacheStrategies,
} from '../../middleware/cacheHeaders'
import {
  handleDomainError,
  handleUnknownError,
} from '../../middleware/errorHandler'
import {
  registerProviders,
  SERVICE_KEYS,
  getHandler,
} from '../../../shared/di/providers'
import { isSuccess } from '../../../shared/types/Result'
import type { GetBlocHandler } from '../../../application/content/handlers/GetBlocHandler'
import type { ContentBloc } from '../../../domain/content/entities/ContentBloc'
import { DomainError } from '../../../shared/errors'

/**
 * Content bloc API endpoint (Clean Architecture)
 */
export default defineEventHandler(async (event): Promise<ContentBloc> => {
  try {
    // 1. Detect language
    const domainLanguage = detectLanguage(event)

    // 2. Register dependencies
    registerProviders(domainLanguage)

    // 3. Apply cache headers
    applyCacheHeaders(event, CacheStrategies.ONE_HOUR)

    // 4. Get blocId from route params
    const blocId = getRouterParam(event, 'blocId')
    if (!blocId) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Bloc ID is required',
      })
    }

    // 5. Get handler from DI container
    const handler = getHandler<GetBlocHandler>(SERVICE_KEYS.GET_BLOC_HANDLER)

    // 6. Execute use case
    const result = await handler.handle({ blocId })

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
