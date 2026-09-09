import { getRequestHeader } from 'h3'

import type { BlogPostDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../../utils/cache-headers'
import { findBlogDocBySlug, toBlogPostDto } from '../../../utils/blog-content'

/**
 * Blog article by slug, served from the `blog` Nuxt Content collection. See articles.ts for why
 * the response shape (BlogPostDto) is preserved unchanged.
 *
 * No extractBackendErrorDetails here (unlike the old front-api-proxying version): that helper is
 * shaped around the generated client's ResponseError and would turn this route's own 404 into a
 * 500. There is no backend call left to translate errors from.
 */
export default defineEventHandler(async (event): Promise<BlogPostDto> => {
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

  const doc = await findBlogDocBySlug(event, slug)
  if (!doc) {
    throw createError({ statusCode: 404, statusMessage: 'Article not found' })
  }
  return await toBlogPostDto(doc)
})
