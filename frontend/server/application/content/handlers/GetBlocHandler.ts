import type { IContentRepository } from '../../../domain/content/repositories/IContentRepository'
import type { GetBlocQuery } from '../queries/GetBlocQuery'
import type { ContentBloc } from '../../../domain/content/entities/ContentBloc'
import type { Result } from '../../../shared/types/Result'
import type { DomainError } from '../../../shared/errors'
import { ValidationError } from '../../../shared/errors'
import { failure } from '../../../shared/types/Result'

/**
 * Handler for GetBloc query
 */
export class GetBlocHandler {
  constructor(private readonly repository: IContentRepository) {}

  async handle(query: GetBlocQuery): Promise<Result<ContentBloc, DomainError>> {
    // Validate blocId
    const blocId = query.blocId?.trim()
    if (!blocId) {
      return failure(new ValidationError('Bloc ID is required'))
    }

    // Delegate to repository
    return await this.repository.getBlocById(blocId)
  }
}
