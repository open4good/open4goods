import { createError, defineEventHandler, getRouterParam } from 'h3'

import type { XwikiContentBlocDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

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
    // is the only source for this endpoint -- front-api's /blocs/{blocId} backend was removed
    // once the coverage check confirmed every bloc id requested by the frontend has a static
    // entry (xwiki-editorial-content-to-nuxt-content AC2). Anonymous XWiki REST access has been
    // down since 2026-09-09 anyway (incident_xwiki_blog_locked_down_sept2026).
    const staticContent = getStaticContentBloc(blocId, domainLanguage)
    if (staticContent === null) {
      throw createError({
        statusCode: 404,
        statusMessage: `No static content for bloc id "${blocId}"`,
      })
    }

    return { blocId, htmlContent: staticContent, editLink: null }
  }
)
