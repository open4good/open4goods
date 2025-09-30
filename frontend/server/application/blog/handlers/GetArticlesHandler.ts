import type { IBlogRepository } from '../../../domain/blog/repositories/IBlogRepository'
import type { GetArticlesQuery } from '../queries/GetArticlesQuery'
import type { Page } from '../../../domain/blog/entities/Page'
import type { Article } from '../../../domain/blog/entities/Article'
import type { Result } from '../../../shared/types/Result'
import type { DomainError } from '../../../shared/errors'

/**
 * Handler for GetArticles query
 * Encapsulates the use case logic for fetching paginated articles
 */
export class GetArticlesHandler {
  constructor(private readonly repository: IBlogRepository) {}

  async handle(
    query: GetArticlesQuery
  ): Promise<Result<Page<Article>, DomainError>> {
    // Apply business rules/validation
    const pageNumber =
      query.pageNumber !== undefined ? Math.max(0, query.pageNumber) : undefined

    const pageSize =
      query.pageSize !== undefined
        ? Math.min(Math.max(1, query.pageSize), 100) // Max 100 items per page
        : undefined

    const tag = query.tag?.trim() || undefined

    // Delegate to repository
    return await this.repository.getArticles({
      pageNumber,
      pageSize,
      tag,
    })
  }
}
