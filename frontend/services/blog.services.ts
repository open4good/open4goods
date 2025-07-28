import {
  BlogApi,
  Configuration,
  type BlogPostDto,
  type PageDto,
} from '~/src/api'
import type {
  BlogArticleData,
  PaginatedBlogResponse,
} from '~/server/api/blog/types/blog.models'

/**
 * Blog service for handling blog-related API calls
 */
export class BlogService {
  /**
   * Instantiate the OpenAPI client with runtime config
   */
  private getApi(): BlogApi {
    const config = useRuntimeConfig()
    return new BlogApi(new Configuration({ basePath: config.public.blogUrl }))
  }

  /**
   * Fetch paginated blog articles using the OpenAPI client
   */
  async getArticles(): Promise<PaginatedBlogResponse> {
    try {
      const api = this.getApi()
      const page: PageDto = await api.posts()

      return {
        page: {
          number: page.page?.number ?? 0,
          size: page.page?.size ?? 10,
          totalElements: page.page?.totalElements ?? 0,
          totalPages: page.page?.totalPages ?? 0,
        },
        data: (page.data ?? []) as BlogArticleData[],
      }
    } catch (error) {
      console.error('Error fetching blog articles:', error)
      throw new Error('Failed to fetch blog articles')
    }
  }

  /**
   * Fetch a single blog article by slug
   */
  async getArticleById(id: string): Promise<BlogArticleData> {
    try {
      const api = this.getApi()
      const article: BlogPostDto = await api.post({ slug: id })
      return article as BlogArticleData
    } catch (error) {
      console.error(`Error fetching blog article ${id}:`, error)
      throw new Error(`Failed to fetch blog article ${id}`)
    }
  }
}

// Export singleton instance
export const blogService = new BlogService()
