import { getRequestHeader } from 'h3'

import type { BlogTagDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { listBlogDocs, toBlogTagDtos } from '../../utils/blog-content'

/**
 * Blog tags API endpoint, served from the `blog` Nuxt Content collection. See articles.ts for why
 * the response shape (BlogTagDto[]) is preserved unchanged.
 */
export default defineEventHandler(async (event): Promise<BlogTagDto[]> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  const rawHost =
    getRequestHeader(event, 'x-forwarded-host') ?? getRequestHeader(event, 'host')
  resolveDomainLanguage(rawHost)

  const docs = await listBlogDocs(event)
  return toBlogTagDtos(docs)
})
