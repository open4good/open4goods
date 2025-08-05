import { useContentService } from '~/services/content.service'
import type { XwikiContentBlocDto } from '~/src/api'
import { handleErrors } from '~/utils'

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
    handleErrors._handleError(error, 'Failed to fetch content bloc')
  }
})
