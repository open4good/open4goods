import { createError, defineEventHandler, getRouterParam } from 'h3'

import type { CmsFullPage } from '~~/shared/utils/cms-full-page'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { getStaticFullPage } from '../../utils/static-full-pages'

export default defineEventHandler(async (event): Promise<CmsFullPage> => {
  const param = getRouterParam(event, 'pageId')
  if (!param) {
    throw createError({ statusCode: 400, statusMessage: 'Page id is required' })
  }

  const pageId = decodeURIComponent(param)
  setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  // Real editorial copy extracted from the XWiki export archive (see static-full-pages.ts)
  // is the only source for this endpoint -- front-api's /pages/{xwikiPageId} backend was removed
  // once the last live-only pages (the 3 unrecoverable /blog/* posts) were dropped and redirected
  // to /blog instead (xwiki-editorial-content-to-nuxt-content AC2, pages slice).
  const staticPage = getStaticFullPage(pageId, domainLanguage)
  if (staticPage === null) {
    throw createError({
      statusCode: 404,
      statusMessage: `No static content for page id "${pageId}"`,
    })
  }

  return staticPage
})
