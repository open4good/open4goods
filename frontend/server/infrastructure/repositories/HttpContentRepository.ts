import type { IContentRepository } from '../../domain/content/repositories/IContentRepository'
import type { ContentBloc } from '../../domain/content/entities/ContentBloc'
import type { Result } from '../../shared/types/Result'
import type { DomainError } from '../../shared/errors'
import { success, failure } from '../../shared/types/Result'
import { NotFoundError, InfrastructureError } from '../../shared/errors'
import { createContentBloc } from '../../domain/content/entities/ContentBloc'
import { useContentService } from '~~/shared/api-client/services/content.services'
import type { DomainLanguage } from '~~/shared/utils/domain-language'
import { ResponseError } from '~~/shared/api-client'

/**
 * HTTP implementation of Content Repository
 */
export class HttpContentRepository implements IContentRepository {
  private readonly contentService: ReturnType<typeof useContentService>
  private readonly domainLanguage: DomainLanguage

  constructor(domainLanguage: DomainLanguage) {
    this.domainLanguage = domainLanguage
    this.contentService = useContentService(domainLanguage)
  }

  async getBlocById(blocId: string): Promise<Result<ContentBloc, DomainError>> {
    try {
      const dto = await this.contentService.getBloc(blocId)

      const contentBloc = createContentBloc({
        id: dto.blocId ?? blocId,
        content: dto.htmlContent ?? '',
        language: this.domainLanguage,
        lastModified: undefined,
      })

      return success(contentBloc)
    } catch (error) {
      return failure(this.handleError(error, blocId))
    }
  }

  private handleError(error: unknown, blocId: string): DomainError {
    if (error instanceof ResponseError) {
      const status = error.response.status

      if (status === 404) {
        return new NotFoundError('content bloc', blocId, error)
      }

      return new InfrastructureError(
        `Failed to fetch content bloc: ${error.response.statusText}`,
        status,
        error
      )
    }

    const message =
      error instanceof Error ? error.message : 'Failed to fetch content bloc'
    return new InfrastructureError(message, 500, error)
  }
}
