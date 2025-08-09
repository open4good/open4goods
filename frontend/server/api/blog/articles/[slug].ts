import { useBlogService } from '~/services/blog.services'
import type { BlogPostDto } from '~/src/api'
import { ResponseError } from '~/src/api'

/**
 * Blog article by slug API endpoint
 * Handles GET requests for a single blog article
 */
export default defineEventHandler(async (event): Promise<BlogPostDto> => {
  // Cache the article for one hour
  setResponseHeader(
    event,
    'Cache-Control',
    'public, max-age=3600, s-maxage=3600'
  )

  const slug = getRouterParam(event, 'slug')

  if (!slug) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Article slug is required',
    })
  }

  const blogService = useBlogService()

  try {
    // Use the service to fetch the article
    const response = await blogService.getArticleBySlug(slug)
    return response
  } catch (error) {
    console.error('Error fetching blog article:', error)

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
        error instanceof Error ? error.message : 'Failed to fetch blog article',
      cause: error,
    })
  }
})
