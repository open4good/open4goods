import { useBlogService } from '~~/shared/api-client/services/blog.services'
import type { BlogTagDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(async (event): Promise<BlogTagDto[]> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const blogService = useBlogService(domainLanguage)

  try {
    return await blogService.getTags()
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)

    console.error(
      'Error fetching blog tags:',
      backendError.logMessage,
      backendError
    )

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
