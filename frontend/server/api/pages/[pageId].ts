import { createError, defineEventHandler, getRouterParam } from 'h3'

import {
  usePagesService,
  type CmsFullPage,
} from '~~/shared/api-client/services/pages.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
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
  // takes over for page ids it covers, entirely bypassing the live XWiki call -- anonymous
  // XWiki REST access has been down since 2026-09-09 (incident_xwiki_blog_locked_down_sept2026).
  const staticPage = getStaticFullPage(pageId, domainLanguage)
  if (staticPage !== null) {
    return staticPage
  }

  const pagesService = usePagesService(domainLanguage)

  try {
    return await pagesService.getPage(pageId)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error('Error fetching page', backendError.logMessage, backendError)

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
