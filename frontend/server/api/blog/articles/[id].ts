import { useBlogService } from '~/services/blog.service'
import type { BlogPostDto } from '~/src/api'
import { ResponseError } from '~/src/api'
import { handleErrors } from '~/utils'

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

  const blogService = useBlogService()

  try {
    // Use the service to fetch the article
    const response = await blogService.getArticleById(slug)
    return response
  } catch (error) {
    let message = 'Failed to fetch blog article'
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
