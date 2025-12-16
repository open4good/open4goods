import { BlogApi } from '..'
import type { BlogPostDto, BlogTagDto, PageDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Blog service for handling blog-related API calls
 */
export const useBlogService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: BlogApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useBlogService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new BlogApi(createBackendApiConfig())
    }

    return api
  }

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
      return await resolveApi().posts({ ...params, domainLanguage })
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
      return await resolveApi().post({ slug, domainLanguage })
    } catch (error) {
      console.error(`Error fetching blog article ${slug}:`, error)
      // Preserve original error details for upstream handlers
      throw error
    }
  }

  /**
   * Fetch available blog tags with post counts
   */
  const getTags = async (): Promise<BlogTagDto[]> => {
    try {
      return await resolveApi().tags({ domainLanguage })
    } catch (error) {
      console.error('Error fetching blog tags:', error)
      throw error
    }
  }

  return { getArticles, getArticleBySlug, getTags }
}
