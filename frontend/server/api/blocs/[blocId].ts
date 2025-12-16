import { useContentService } from '~~/shared/api-client/services/content.services'
import type { XwikiContentBlocDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(
  async (event): Promise<XwikiContentBlocDto> => {
    const blocId = getRouterParam(event, 'blocId')
    if (!blocId) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Bloc id is required',
      })
    }

    // Cache content for 1 hour
    setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const contentService = useContentService(domainLanguage)
    try {
      return await contentService.getBloc(blocId)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error fetching bloc',
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
)
