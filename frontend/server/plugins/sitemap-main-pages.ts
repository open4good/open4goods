import { getRequestURL } from 'h3'

import { APP_ROUTES_SITEMAP_KEY } from '~~/shared/utils/sitemap-config'
import { getDomainLanguageFromHostname } from '~~/shared/utils/domain-language'
import { getMainPagePathsForDomainLanguage } from '~~/shared/utils/sitemap-main-pages'

const MAIN_PAGES_SITEMAP_FILENAME = `${APP_ROUTES_SITEMAP_KEY}.xml`

export default defineNitroPlugin(nitroApp => {
  nitroApp.hooks.hook('sitemap:sources', ctx => {
    if (
      ctx.sitemapName !== MAIN_PAGES_SITEMAP_FILENAME &&
      ctx.sitemapName !== APP_ROUTES_SITEMAP_KEY
    ) {
      return
    }

    const requestURL = ctx.event
      ? getRequestURL(ctx.event)
      : new URL('https://nudger.fr')
    const { domainLanguage } = getDomainLanguageFromHostname(
      requestURL.hostname
    )

    const staticPaths = Array.from(
      new Set(getMainPagePathsForDomainLanguage(domainLanguage))
    )

    if (!staticPaths.length) {
      return
    }

    ctx.sources.push({
      context: {
        name: 'static-main-pages',
        description: 'Autodiscovered localized marketing routes',
      },
      sourceType: 'user',
      urls: staticPaths.map(path => ({ loc: path })),
    })
  })
})
