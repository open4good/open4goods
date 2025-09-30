import type { IBlogRepository } from '../../domain/blog/repositories/IBlogRepository'
import type { Article } from '../../domain/blog/entities/Article'
import type { Tag } from '../../domain/blog/entities/Tag'
import type { Page } from '../../domain/blog/entities/Page'
import type { Result } from '../../shared/types/Result'
import type { DomainError } from '../../shared/errors'
import { success, failure } from '../../shared/types/Result'
import { NotFoundError, InfrastructureError } from '../../shared/errors'
import { createArticle } from '../../domain/blog/entities/Article'
import { createTag } from '../../domain/blog/entities/Tag'
import { createPage } from '../../domain/blog/entities/Page'
import { useBlogService } from '~~/shared/api-client/services/blog.services'
import type { DomainLanguage } from '~~/shared/utils/domain-language'
import { ResponseError } from '~~/shared/api-client'

/**
 * HTTP implementation of Blog Repository
 * Adapter that connects domain logic to external HTTP API
 */
export class HttpBlogRepository implements IBlogRepository {
  private readonly blogService: ReturnType<typeof useBlogService>

  constructor(domainLanguage: DomainLanguage) {
    this.blogService = useBlogService(domainLanguage)
  }

  async getArticles(params: {
    pageNumber?: number
    pageSize?: number
    tag?: string
  }): Promise<Result<Page<Article>, DomainError>> {
    try {
      const response = await this.blogService.getArticles(params)

      // Transform DTOs to domain entities
      const articles = (response.data ?? []).map(dto =>
        createArticle({
          id: dto.url ?? '',
          slug: dto.url ?? '',
          title: dto.title ?? '',
          excerpt: dto.summary ?? '',
          content: dto.body ?? '',
          author: dto.author ?? 'Unknown',
          publishedAt: dto.createdMs
            ? new Date(dto.createdMs).toISOString()
            : new Date().toISOString(),
          updatedAt: dto.modifiedMs
            ? new Date(dto.modifiedMs).toISOString()
            : new Date().toISOString(),
          tags: dto.category ?? [],
          imageUrl: dto.image,
          readTime: undefined,
        })
      )

      const page = createPage(articles, {
        number: response.page?.number ?? 0,
        size: response.page?.size ?? params.pageSize ?? 10,
        totalElements: response.page?.totalElements ?? articles.length,
        totalPages: response.page?.totalPages ?? 1,
      })

      return success(page)
    } catch (error) {
      return failure(this.handleError(error, 'articles'))
    }
  }

  async getArticleBySlug(slug: string): Promise<Result<Article, DomainError>> {
    try {
      const dto = await this.blogService.getArticleBySlug(slug)

      const article = createArticle({
        id: dto.url ?? slug,
        slug: dto.url ?? slug,
        title: dto.title ?? '',
        excerpt: dto.summary ?? '',
        content: dto.body ?? '',
        author: dto.author ?? 'Unknown',
        publishedAt: dto.createdMs
          ? new Date(dto.createdMs).toISOString()
          : new Date().toISOString(),
        updatedAt: dto.modifiedMs
          ? new Date(dto.modifiedMs).toISOString()
          : new Date().toISOString(),
        tags: dto.category ?? [],
        imageUrl: dto.image,
        readTime: undefined,
      })

      return success(article)
    } catch (error) {
      return failure(this.handleError(error, 'article', slug))
    }
  }

  async getTags(): Promise<Result<Tag[], DomainError>> {
    try {
      const dtos = await this.blogService.getTags()

      const tags = dtos.map(dto =>
        createTag({
          name: dto.name ?? '',
          slug: dto.name ?? '',
          count: dto.count ?? 0,
        })
      )

      return success(tags)
    } catch (error) {
      return failure(this.handleError(error, 'tags'))
    }
  }

  /**
   * Handle errors from the HTTP layer and convert to domain errors
   */
  private handleError(
    error: unknown,
    resource: string,
    identifier?: string
  ): DomainError {
    if (error instanceof ResponseError) {
      const status = error.response.status

      // 404 errors become NotFoundError
      if (status === 404 && identifier) {
        return new NotFoundError(resource, identifier, error)
      }

      // Other HTTP errors become InfrastructureError
      return new InfrastructureError(
        `Failed to fetch ${resource}: ${error.response.statusText}`,
        status,
        error
      )
    }

    // Unknown errors
    const message =
      error instanceof Error ? error.message : `Failed to fetch ${resource}`
    return new InfrastructureError(message, 500, error)
  }
}
