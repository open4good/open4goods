import { BlogApi } from '..'
import type { BlogPostDto, BlogTagDto, PageDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'
import { withRetry } from './withRetry'

/**
 * Blog service for handling blog-related API calls
 */
export const useBlogService = (domainLanguage: DomainLanguage) => {
  const isVitest = typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: BlogApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error('useBlogService() is only available on the server runtime.')
    }

    if (!api) {
      api = new BlogApi(createBackendApiConfig())
    }

    return api
  }

  /**
   * Fetch paginated blog articles with retry logic
   * @returns Promise<PageDto>
   */
  const getArticles = async (
    params: {
      tag?: string
      pageNumber?: number
      pageSize?: number
    } = {}
  ): Promise<PageDto> => {
    return withRetry(
      () => resolveApi().posts({ ...params, domainLanguage }),
      2
    ).catch(error => {
      console.error('Error fetching blog articles after retries:', error)
      throw error
    })
  }

  /**
   * Fetch a single blog article by slug with retry logic
   * @param slug - Article slug
   * @returns Promise<BlogPostDto>
   */
  const getArticleBySlug = async (slug: string): Promise<BlogPostDto> => {
    return withRetry(
      () => resolveApi().post({ slug, domainLanguage }),
      2
    ).catch(error => {
      console.error(`Error fetching blog article ${slug} after retries:`, error)
      throw error
    })
  }

  /**
   * Fetch available blog tags with post counts and retry logic
   */
  const getTags = async (): Promise<BlogTagDto[]> => {
    return withRetry(
      () => resolveApi().tags({ domainLanguage }),
      2
    ).catch(error => {
      console.error('Error fetching blog tags after retries:', error)
      throw error
    })
  }

  return { getArticles, getArticleBySlug, getTags }
}
