import { BlogApi, Configuration } from '..'
import type { BlogPostDto, PageDto } from '..'
import { getCurrentDomainLanguage } from '~~/shared/utils/domain-language'

/**
 * Blog service for handling blog-related API calls
 */
export const useBlogService = () => {
  const config = useRuntimeConfig()
  const apiConfig = new Configuration({ basePath: config.apiUrl })
  console.log('[ContentService] baseUrl =', config.apiUrl)

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
      const { tag, pageNumber, pageSize } = params
      return await api.posts({
        tag,
        pageNumber,
        pageSize,
        domainLanguage: getCurrentDomainLanguage(),
      })
    } catch (error) {
      console.error('Error fetching blog articles:', error)
      // Rethrow original error so callers can access status and message
      throw error
    }
  }

  /**
   * Fetch a single blog article by slug
   * @param slug - Article slug
   * @returns Promise<BlogPostDto>
   */
  const getArticleBySlug = async (slug: string): Promise<BlogPostDto> => {
    try {
      return await api.post({
        slug,
        domainLanguage: getCurrentDomainLanguage(),
      })
    } catch (error) {
      console.error(`Error fetching blog article ${slug}:`, error)
      // Preserve original error details for upstream handlers
      throw error
    }
  }

  return { getArticles, getArticleBySlug }
}
