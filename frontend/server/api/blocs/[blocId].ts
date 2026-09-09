import { createError, defineEventHandler, getRouterParam } from 'h3'

import { useContentService } from '~~/shared/api-client/services/content.services'
import type { XwikiContentBlocDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { getStaticContentBloc } from '../../utils/static-content-blocs'

export default defineEventHandler(
  async (event): Promise<XwikiContentBlocDto> => {
    const blocId = getRouterParam(event, 'blocId')
    if (!blocId) {
      throw createError({
        statusCode: 400,
        statusMessage: 'Bloc id is required',
      })
    }

    // Cache content for 1 hour
    setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')
    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    // Real editorial copy extracted from the XWiki export archive (see static-content-blocs.ts)
    // takes over for bloc ids it covers, entirely bypassing the live XWiki call -- anonymous
    // XWiki REST access has been down since 2026-09-09 (incident_xwiki_blog_locked_down_sept2026).
    const staticContent = getStaticContentBloc(blocId, domainLanguage)
    if (staticContent !== null) {
      return { blocId, htmlContent: staticContent, editLink: null }
    }

    const contentService = useContentService(domainLanguage)
    try {
      return await contentService.getBloc(blocId)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error fetching bloc',
        backendError.logMessage,
        backendError
      )

      throw createError({
        statusCode: backendError.statusCode,
        statusMessage: backendError.statusMessage,
        cause: error,
      })
    }
  }
)
