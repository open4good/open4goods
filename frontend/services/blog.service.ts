import { BlogApi, Configuration } from '~/src/api'
import type { BlogPostDto, PageDto } from '~/src/api'
import { handleErrors } from '~/utils'

/**
 * Blog service for handling blog-related API calls
 */
export const useBlogService = () => {
  const config = useRuntimeConfig()
  const apiConfig = new Configuration({ basePath: config.apiUrl })
  const api = new BlogApi(apiConfig)

  /**
   * Fetch paginated blog articles
   * @returns Promise<PageDto>
   */
  const getArticles = async (
    params: {
      tag?: string
      pageNumber?: number
      pageSize?: number
    } = {}
  ): Promise<PageDto> => {
    try {
      return await api.posts(params)
    } catch (error) {
      handleErrors._handleError(error, 'Error fetching blog articles')
    }
  }

  /**
   * Fetch a single blog article by slug
   * @param slug - Article slug
   * @returns Promise<BlogPostDto>
   */
  const getArticleById = async (slug: string): Promise<BlogPostDto> => {
    try {
      return await api.post({ slug })
    } catch (error) {
      handleErrors._handleError(error, `Error fetching blog article ${slug}`)
    }
  }

  return { getArticles, getArticleById }
}
