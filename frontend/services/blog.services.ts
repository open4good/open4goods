import type {
  BlogArticleData,
  PaginatedBlogResponse,
} from '~/server/api/blog/types/blog.models'

/**
 * Blog service for handling blog-related API calls
 */
export class BlogService {
  private readonly baseUrl =
    process.env.NUXT_PUBLIC_SITE_URL || 'http://localhost:3000'
  private readonly blogEndpoint = '/blog/posts'

  /**
   * Fetch paginated blog articles
   *
   * @param pageNumber - zero based page index
   * @param pageSize - page size
   * @returns Promise<PaginatedBlogResponse>
   */
  async getArticles(
    pageNumber = 0,
    pageSize = 10
  ): Promise<PaginatedBlogResponse> {
    try {
      const response = await $fetch<PaginatedBlogResponse>(
        `${this.baseUrl}${this.blogEndpoint}`,
        {
          params: { pageNumber, pageSize },
        }
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
