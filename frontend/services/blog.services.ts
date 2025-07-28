import type {
  BlogArticleData,
  PaginatedBlogResponse,
} from '~/server/api/blog/types/blog.models'
import { BlogApi, Configuration } from '~/src/api'

/**
 * Blog service for handling blog-related API calls
 */
export class BlogService {
  private readonly api: BlogApi

  constructor(api?: BlogApi) {
    if (api) {
      this.api = api
    } else {
      const baseUrl =
        process.env.BLOG_URL || 'https://beta.front-api.nudger.fr'
      this.api = new BlogApi(new Configuration({ basePath: baseUrl }))
    }
  }

  /**
   * Fetch paginated blog articles
   * @returns Promise<PaginatedBlogResponse>
   */
  async getArticles(): Promise<PaginatedBlogResponse> {
    try {
      const response = await this.api.posts()
      return response as unknown as PaginatedBlogResponse
    } catch (error) {
      console.error('Error fetching blog articles:', error)
      throw new Error('Failed to fetch blog articles')
    }
  }

  /**
   * Fetch a single blog article by ID
   * @param id - Article ID
   * @returns Promise<BlogArticleData>
   */
  async getArticleById(id: string): Promise<BlogArticleData> {
    try {
      const response = await this.api.post({ slug: id })
      return response as unknown as BlogArticleData
    } catch (error) {
      console.error(`Error fetching blog article ${id}:`, error)
      throw new Error(`Failed to fetch blog article ${id}`)
    }
  }
}

// Export singleton instance
export const blogService = new BlogService()
