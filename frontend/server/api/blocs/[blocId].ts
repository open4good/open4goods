import { contentService } from '~/services/content.services'
import type { XwikiContentBlocDto } from '~/src/api'
import { _handleError } from '~/utils/server/_handdleErrors'

export default defineEventHandler(async (event): Promise<XwikiContentBlocDto> => {
  const blocId = getRouterParam(event, 'blocId')
  if (!blocId) {
    throw createError({ statusCode: 400, statusMessage: 'Bloc id is required' })
  }

  // Cache content for 1 hour
  setResponseHeader(event, 'Cache-Control', 'public, max-age=3600, s-maxage=3600')
  try {
    return await contentService.getBloc(blocId)
  } catch (error) {
    _handleError(error, 'Failed to fetch content bloc')
  }
})
