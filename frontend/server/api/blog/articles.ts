import { getQuery, getRequestHeader } from 'h3'

import type { PageDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { listBlogDocs, toBlogPostDto } from '../../utils/blog-content'

/**
 * Blog articles API endpoint, served from the `blog` Nuxt Content collection
 * (frontend/content/blog) instead of XWiki via front-api's PostsController.
 * Response shape (PageDto<BlogPostDto>) is unchanged so useBlog.ts and every
 * downstream component keep working without modification.
 */
export default defineEventHandler(async (event): Promise<PageDto> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  // Resolved for parity with the other blog routes and future domainLanguage-driven
  // filtering; the collection is French-only today so it isn't used to filter yet.
  // h3's getRequestHeader (not the raw event.node.req.headers the old front-api-proxying
  // version used) since this route has no backend call left to justify depending on the
  // Node-specific request shape.
  const rawHost =
    getRequestHeader(event, 'x-forwarded-host') ?? getRequestHeader(event, 'host')
  resolveDomainLanguage(rawHost)

  const query = getQuery(event)
  const pageNumberParam = Array.isArray(query.pageNumber)
    ? query.pageNumber[0]
    : query.pageNumber
  const pageSizeParam = Array.isArray(query.pageSize)
    ? query.pageSize[0]
    : query.pageSize
  const tagParam = Array.isArray(query.tag) ? query.tag[0] : query.tag

  const pageNumber = pageNumberParam ? Number.parseInt(pageNumberParam, 10) : 0
  const pageSize = pageSizeParam ? Number.parseInt(pageSizeParam, 10) : 20
  const safePageNumber = Number.isNaN(pageNumber) || pageNumber < 0 ? 0 : pageNumber
  const safePageSize = Number.isNaN(pageSize) || pageSize < 1 ? 20 : pageSize
  const tag = typeof tagParam === 'string' && tagParam.trim() ? tagParam.trim() : undefined

  const allDocs = await listBlogDocs(event)
  const filtered = tag ? allDocs.filter(doc => doc.tags.includes(tag)) : allDocs

  const start = safePageNumber * safePageSize
  const pageDocs = filtered.slice(start, start + safePageSize)
  const data = await Promise.all(pageDocs.map(toBlogPostDto))

  const totalElements = filtered.length
  const totalPages = safePageSize > 0 ? Math.ceil(totalElements / safePageSize) : 0

  return {
    page: {
      number: safePageNumber,
      size: safePageSize,
      totalElements,
      totalPages,
    },
    data,
  }
})
