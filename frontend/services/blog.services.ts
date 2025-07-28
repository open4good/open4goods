import { BlogApi } from '@/api/apis/BlogApi'
import { Configuration } from '@/api'
import type { BlogPostDto, BlogTagDto, PageDto } from '@/api/models'
import { useRuntimeConfig } from '#app'

/**
 * Blog service for handling blog-related API calls
 */
export class BlogService {
  private readonly api: BlogApi

  constructor() {
    const config = useRuntimeConfig()
    const headers: Record<string, string> = {}
    if (config.blogToken) {
      headers['Authorization'] = `Bearer ${config.blogToken}`
    }
    const apiConfig = new Configuration({
      basePath: config.public.blogUrl,
      headers,
    })
    this.api = new BlogApi(apiConfig)
  }

  /**
   * Fetch paginated blog articles
   */
  async getArticles(): Promise<PageDto> {
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
  async getArticleById(slug: string): Promise<BlogPostDto> {
    try {
      return await this.api.post({ slug })
    } catch (error) {
      console.error(`Error fetching blog article ${slug}:`, error)
      throw new Error(`Failed to fetch blog article ${slug}`)
    }
  }

  /**
   * List available blog tags
   */
  async getTags(): Promise<BlogTagDto[]> {
    try {
      return await this.api.tags()
    } catch (error) {
      console.error('Error fetching blog tags:', error)
      throw new Error('Failed to fetch blog tags')
    }
  }
}

// Export singleton instance
export const blogService = new BlogService()
