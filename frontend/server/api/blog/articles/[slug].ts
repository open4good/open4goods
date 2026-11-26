import { getRequestHeader, setResponseHeader } from 'h3'

import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'
import {
  findBlogDocBySlug,
  toBlogPostDto,
  type BlogArticleDto,
} from '../../../utils/blog-content'

/**
 * Blog article by slug, served from the `blog` Nuxt Content collection. See articles.ts for why
 * the response shape (BlogPostDto) is preserved unchanged.
 *
 * No extractBackendErrorDetails here (unlike the old front-api-proxying version): that helper is
 * shaped around the generated client's ResponseError and would turn this route's own 404 into a
 * 500. There is no backend call left to translate errors from.
 *
 * Drafts: with the `blogShowDrafts` runtime config (NUXT_BLOG_SHOW_DRAFTS=true, beta only), a
 * `draft: true` post is also served here, flagged `draft: true` and never cached, so that it can be
 * reviewed at its URL. It stays out of every listing, the sitemap and the RSS feed.
 */
export default defineEventHandler(async (event): Promise<BlogArticleDto> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  const slug = getRouterParam(event, 'slug')
  if (!slug) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Article slug is required',
    })
  }

  const rawHost =
    getRequestHeader(event, 'x-forwarded-host') ?? getRequestHeader(event, 'host')
  resolveDomainLanguage(rawHost)

  const { blogShowDrafts } = useRuntimeConfig(event)
  const doc = await findBlogDocBySlug(event, slug, {
    includeDrafts: blogShowDrafts === true || blogShowDrafts === 'true',
  })
  if (!doc) {
    throw createError({ statusCode: 404, statusMessage: 'Article not found' })
  }
  if (doc.draft) {
    setResponseHeader(event, 'Cache-Control', 'private, no-store')
    return { ...(await toBlogPostDto(doc)), draft: true }
  }
  return await toBlogPostDto(doc)
})
