import type { BlogPostDto, PageDto } from '~/src/api'


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
   * @returns Promise<PageDto>
   */
  async getArticles(): Promise<PageDto> {
    try {
      const response = await $fetch<PageDto>(
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
   * @returns Promise<BlogPostDto>
   */
  async getArticleById(id: string): Promise<BlogPostDto> {
    try {
      const response = await $fetch<BlogPostDto>(
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
