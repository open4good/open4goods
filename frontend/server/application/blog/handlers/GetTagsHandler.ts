import type { IBlogRepository } from '../../../domain/blog/repositories/IBlogRepository'
import type { GetTagsQuery } from '../queries/GetTagsQuery'
import type { Tag } from '../../../domain/blog/entities/Tag'
import type { Result } from '../../../shared/types/Result'
import type { DomainError } from '../../../shared/errors'

/**
 * Handler for GetTags query
 * Encapsulates the use case logic for fetching all tags
 */
export class GetTagsHandler {
  constructor(private readonly repository: IBlogRepository) {}

  async handle(_query: GetTagsQuery): Promise<Result<Tag[], DomainError>> {
    // No validation needed for now
    // Delegate to repository
    return await this.repository.getTags()
  }
}
