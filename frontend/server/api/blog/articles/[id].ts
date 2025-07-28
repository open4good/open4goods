import { blogService } from '~/services/blog.services'
import type { BlogPostDto } from '~/src/api'
import { _handleError } from '~/utils/server/_handdleErrors'

/**
 * Blog article by ID API endpoint
 * Handles GET requests for a single blog article
 */
export default defineEventHandler(async (event): Promise<BlogPostDto> => {
  const slug = getRouterParam(event, 'id')

  if (!slug) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Article ID is required',
    })
  }

  try {
    // Use the service to fetch the article
    const response = await blogService.getArticleById(slug)
    return response
  } catch (error) {
    _handleError(error, 'Failed to fetch blog article')
  }
})
