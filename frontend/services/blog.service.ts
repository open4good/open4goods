import { BlogApi, Configuration } from '~/src/api'

/**
 * Blog service for handling blog-related API calls
 */
export class BlogService {
  private readonly api: BlogApi

  constructor() {
    const config = useRuntimeConfig()
    this.api = new BlogApi(
      new Configuration({ basePath: config.public.blogUrl })
    )
  }

  /**
   * Fetch paginated blog articles from the API
   */
  async getArticles() {
    try {
      return await this.api.posts()
    } catch (error) {
      console.error('Error fetching blog articles:', error)
      throw new Error('Failed to fetch blog articles')
    }
  }

  /**
   * Fetch a single blog article by slug
   */
  async getArticleById(slug: string) {
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
