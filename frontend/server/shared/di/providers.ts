import { container } from './container'
import type { DomainLanguage } from '~~/shared/utils/domain-language'
import { HttpBlogRepository } from '../../infrastructure/repositories/HttpBlogRepository'
import { GetArticlesHandler } from '../../application/blog/handlers/GetArticlesHandler'
import { GetArticleBySlugHandler } from '../../application/blog/handlers/GetArticleBySlugHandler'
import { GetTagsHandler } from '../../application/blog/handlers/GetTagsHandler'
import { HttpContentRepository } from '../../infrastructure/repositories/HttpContentRepository'
import { GetBlocHandler } from '../../application/content/handlers/GetBlocHandler'

/**
 * Service keys for dependency injection
 */
export const SERVICE_KEYS = {
  // Repositories
  BLOG_REPOSITORY: 'blog.repository',
  CONTENT_REPOSITORY: 'content.repository',
  AUTH_REPOSITORY: 'auth.repository',

  // Handlers
  GET_ARTICLES_HANDLER: 'blog.handlers.getArticles',
  GET_ARTICLE_BY_SLUG_HANDLER: 'blog.handlers.getArticleBySlug',
  GET_TAGS_HANDLER: 'blog.handlers.getTags',
  GET_BLOC_HANDLER: 'content.handlers.getBloc',
  LOGIN_HANDLER: 'auth.handlers.login',
  REFRESH_HANDLER: 'auth.handlers.refresh',
  LOGOUT_HANDLER: 'auth.handlers.logout',
} as const

/**
 * Helper to register all providers
 * This will be called lazily when needed
 */
export const registerProviders = (domainLanguage: DomainLanguage) => {
  // Register Blog Repository
  if (!container.has(SERVICE_KEYS.BLOG_REPOSITORY)) {
    container.register(
      SERVICE_KEYS.BLOG_REPOSITORY,
      () => new HttpBlogRepository(domainLanguage)
    )
  }

  // Register Blog Handlers
  if (!container.has(SERVICE_KEYS.GET_ARTICLES_HANDLER)) {
    container.register(SERVICE_KEYS.GET_ARTICLES_HANDLER, () => {
      const repository = container.get(SERVICE_KEYS.BLOG_REPOSITORY)
      return new GetArticlesHandler(repository)
    })
  }

  if (!container.has(SERVICE_KEYS.GET_ARTICLE_BY_SLUG_HANDLER)) {
    container.register(SERVICE_KEYS.GET_ARTICLE_BY_SLUG_HANDLER, () => {
      const repository = container.get(SERVICE_KEYS.BLOG_REPOSITORY)
      return new GetArticleBySlugHandler(repository)
    })
  }

  if (!container.has(SERVICE_KEYS.GET_TAGS_HANDLER)) {
    container.register(SERVICE_KEYS.GET_TAGS_HANDLER, () => {
      const repository = container.get(SERVICE_KEYS.BLOG_REPOSITORY)
      return new GetTagsHandler(repository)
    })
  }

  // Register Content Repository
  if (!container.has(SERVICE_KEYS.CONTENT_REPOSITORY)) {
    container.register(
      SERVICE_KEYS.CONTENT_REPOSITORY,
      () => new HttpContentRepository(domainLanguage)
    )
  }

  // Register Content Handlers
  if (!container.has(SERVICE_KEYS.GET_BLOC_HANDLER)) {
    container.register(SERVICE_KEYS.GET_BLOC_HANDLER, () => {
      const repository = container.get(SERVICE_KEYS.CONTENT_REPOSITORY)
      return new GetBlocHandler(repository)
    })
  }
}

/**
 * Helper to get a handler from the container
 */
export const getHandler = <T>(key: string): T => {
  return container.get<T>(key)
}

export { container }