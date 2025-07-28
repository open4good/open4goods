import { useRuntimeConfig } from '#imports'
import type {
  BlogArticleData,
  PaginatedBlogResponse,
} from '~/server/api/blog/types/blog.models'

/**
 * Blog service for handling blog-related API calls
 */

export class BlogService {
  private readonly config = useRuntimeConfig()
  private readonly baseUrl = this.config.public.blogUrl
  private readonly blogEndpoint = '/blog/posts'

  /**
   * Fetch paginated blog articles
   * @returns Promise<PaginatedBlogResponse>
   */
  async getArticles(): Promise<PaginatedBlogResponse> {
    try {
      const response = await $fetch<PaginatedBlogResponse>(
        `${this.baseUrl}${this.blogEndpoint}`
      )
      return response
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
      const response = await $fetch<BlogArticleData>(
        `${this.baseUrl}${this.blogEndpoint}/${id}`
      )
      return response
    } catch (error) {
      console.error(`Error fetching blog article ${id}:`, error)
      throw new Error(`Failed to fetch blog article ${id}`)
    }
  }
}

// Export singleton instance
export const blogService = new BlogService()
