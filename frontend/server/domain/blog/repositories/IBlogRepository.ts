import type { Article } from '../entities/Article'
import type { Tag } from '../entities/Tag'
import type { Page } from '../entities/Page'
import type { Result } from '../../../shared/types/Result'
import type { DomainError } from '../../../shared/errors'

/**
 * Blog Repository Interface (Port)
 * Defines the contract for blog data access
 * Implementation details are hidden in the infrastructure layer
 */
export interface IBlogRepository {
  /**
   * Fetch paginated articles
   * @param params - Query parameters for filtering and pagination
   * @returns Result containing paginated articles or error
   */
  getArticles(params: {
    pageNumber?: number
    pageSize?: number
    tag?: string
  }): Promise<Result<Page<Article>, DomainError>>

  /**
   * Fetch a single article by slug
   * @param slug - Article slug
   * @returns Result containing article or error
   */
  getArticleBySlug(slug: string): Promise<Result<Article, DomainError>>

  /**
   * Fetch all available tags
   * @returns Result containing array of tags or error
   */
  getTags(): Promise<Result<Tag[], DomainError>>
}
