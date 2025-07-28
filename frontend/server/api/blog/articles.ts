import { blogService } from '~/services/blog.services'
import type { PageDto } from '~/src/api'
import { _handleError } from '~/utils/server/_handdleErrors'

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
      _handleError(error, 'Failed to fetch blog articles')
    }
  }
)
