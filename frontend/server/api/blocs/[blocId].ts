import { useContentService } from '~~/shared/api-client/services/content.services'
import type { XwikiContentBlocDto } from '~~/shared/api-client'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'

export default defineEventHandler(async (event): Promise<XwikiContentBlocDto> => {
  const blocId = getRouterParam(event, 'blocId')
  if (!blocId) {
    throw createError({ statusCode: 400, statusMessage: 'Bloc id is required' })
  }

  // Cache content for 1 hour
  setResponseHeader(event, 'Cache-Control', 'public, max-age=3600, s-maxage=3600')
  const contentService = useContentService()
  try {
    return await contentService.getBloc(blocId)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error('Error fetching bloc', backendError.logMessage, backendError)

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
