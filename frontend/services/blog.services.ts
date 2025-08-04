import { BlogApi, Configuration } from '~/src/api'
import type { BlogPostDto, PageDto } from '~/src/api'

/**
 * Blog service for handling blog-related API calls
 */
export class BlogService {
  private readonly api: BlogApi

  constructor(api: BlogApi) {
    this.api = api
  }

  /**
   * Fetch paginated blog articles
   * @returns Promise<PageDto>
   */
  async getArticles(
    params: {
      tag?: string
      pageNumber?: number
      pageSize?: number
    } = {}
  ): Promise<PageDto> {
    try {
      return await this.api.posts(params)
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
  async getArticleById(slug: string): Promise<BlogPostDto> {
    try {
      return await this.api.post({ slug })
    } catch (error) {
      console.error(`Error fetching blog article ${slug}:`, error)
      // Preserve original error details for upstream handlers
      throw error
    }
  }
}

/**
 * Factory to create a BlogService with runtime configuration
 */
export const useBlogService = (): BlogService => {
  const config = useRuntimeConfig()
  const apiConfig = new Configuration({ basePath: config.apiUrl })
  console.log('[BlogService] baseUrl =', config.apiUrl)

  const api = new BlogApi(apiConfig)
  return new BlogService(api)
}
