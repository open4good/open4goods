import { blogService } from '~/services/blog.services'
import type { BlogPostDto } from '~/src/api/models'

/**
 * Blog article by ID API endpoint
 * Handles GET requests for a single blog article
 */
export default defineEventHandler(async (event): Promise<BlogPostDto> => {
  const id = getRouterParam(event, 'id')

  if (!id) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Article ID is required',
    })
  }

  try {
    // Use the service to fetch the article
    const response = await blogService.getArticleById(id)
    return response
  } catch (error) {
    console.error('Error fetching blog article:', error)

    throw createError({
      statusCode: 500,
      statusMessage: 'Failed to fetch blog article',
      cause: error,
    })
  }
})
