import type { IBlogRepository } from '../../../domain/blog/repositories/IBlogRepository'
import type { GetArticleBySlugQuery } from '../queries/GetArticleBySlugQuery'
import type { Article } from '../../../domain/blog/entities/Article'
import type { Result } from '../../../shared/types/Result'
import type { DomainError } from '../../../shared/errors'
import { ValidationError } from '../../../shared/errors'
import { failure } from '../../../shared/types/Result'

/**
 * Handler for GetArticleBySlug query
 * Encapsulates the use case logic for fetching a single article
 */
export class GetArticleBySlugHandler {
  constructor(private readonly repository: IBlogRepository) {}

  async handle(
    query: GetArticleBySlugQuery
  ): Promise<Result<Article, DomainError>> {
    // Validate slug
    const slug = query.slug?.trim()
    if (!slug) {
      return failure(new ValidationError('Article slug is required'))
    }

    // Delegate to repository
    return await this.repository.getArticleBySlug(slug)
  }
}
