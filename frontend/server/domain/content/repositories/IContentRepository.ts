import type { ContentBloc } from '../entities/ContentBloc'
import type { Result } from '../../../shared/types/Result'
import type { DomainError } from '../../../shared/errors'

/**
 * Content Repository Interface (Port)
 */
export interface IContentRepository {
  /**
   * Fetch a content bloc by its ID
   * @param blocId - Content bloc identifier
   * @returns Result containing content bloc or error
   */
  getBlocById(blocId: string): Promise<Result<ContentBloc, DomainError>>
}
