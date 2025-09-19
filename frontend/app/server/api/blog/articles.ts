import { useBlogService } from '~/services/blog.services'
import type { PageDto } from '~/src/api'
import { ResponseError } from '~/src/api'

/**
 * Blog articles API endpoint
 * Handles GET requests for blog articles with caching
 */
export default defineEventHandler(async (event): Promise<PageDto> => {
  // Set cache headers for 1 hour
  setResponseHeader(
    event,
    'Cache-Control',
    'public, max-age=3600, s-maxage=3600'
  )

  const blogService = useBlogService()

  try {
    // Use the service to fetch articles
    const response = await blogService.getArticles()
    return response
  } catch (error) {
    // Log the error for debugging
    console.error('Error fetching blog articles:', error)

    if (error instanceof ResponseError) {
      const message = await error.response.text().catch(() => undefined)

      // Forward backend status code and message
      throw createError({
        statusCode: error.response.status,
        statusMessage: message || error.response.statusText,
        cause: error,
      })
    }

    // Fallback generic error
    throw createError({
      statusCode: 500,
      statusMessage:
        error instanceof Error
          ? error.message
          : 'Failed to fetch blog articles',
      cause: error,
    })
  }
})
