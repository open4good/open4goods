import { blogService } from '~/services/blog.services'
import type { PageDto } from '~/src/api'

/**
 * Blog articles API endpoint
 * Handles GET requests for blog articles with caching
 */
export default defineEventHandler(
  async (event): Promise<PageDto> => {
    // Set cache headers for 1 hour
    setResponseHeader(
      event,
      'Cache-Control',
      'public, max-age=3600, s-maxage=3600'
    )

    try {
      // Use the service to fetch articles
      const response = await blogService.getArticles()
      return response
    } catch (error) {
      // Log the error for debugging
      console.error('Error fetching blog articles:', error)

      // Throw a proper HTTP error
      throw createError({
        statusCode: 500,
        statusMessage: 'Failed to fetch blog articles',
        cause: error,
      })
    }
  }
)
