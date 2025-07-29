import { BlogApi, Configuration } from '~/src/api'
import type { BlogPostDto, PageDto } from '~/src/api'

/**
 * Blog service for handling blog-related API calls
 */
export class BlogService {
  private readonly api: BlogApi

  constructor() {
    const config = useRuntimeConfig()
    const apiConfig = new Configuration({ basePath: config.public.apiUrl })
    this.api = new BlogApi(apiConfig)
  }

  /**
   * Fetch paginated blog articles
   * @returns Promise<PageDto>
   */
  async getArticles(params: {
    tag?: string
    pageNumber?: number
    pageSize?: number
  } = {}): Promise<PageDto> {
    try {
      return await this.api.posts(params)
    } catch (error) {
      console.error('Error fetching blog articles:', error)
      throw new Error('Failed to fetch blog articles')
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
      throw new Error(`Failed to fetch blog article ${slug}`)
    }
  }
}

// Export singleton instance
export const blogService = new BlogService()
