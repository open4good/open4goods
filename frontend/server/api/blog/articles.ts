import { useBlogService } from '~/services/blog.service'
import type { PageDto } from '~/src/api'
import { ResponseError } from '~/src/api'
import { handleErrors } from '~/utils'

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
    let message = 'Failed to fetch blog articles'
    if (error instanceof ResponseError) {
      message =
        (await error.response.text().catch(() => undefined)) ||
        error.response.statusText
    } else if (error instanceof Error) {
      message = error.message
    }
    handleErrors._handleError(error, message)
  }
})
