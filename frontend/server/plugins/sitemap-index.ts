import { getRequestURL } from 'h3'

import { getDomainLanguageFromHostname } from '~~/shared/utils/domain-language'
import { getAdditionalSitemapUrlsForDomainLanguage } from '~~/shared/utils/sitemap-config'

export default (nitroApp: import('nitro/app').NitroApp) => {
  nitroApp.hooks.hook('sitemap:index-resolved', (ctx) => {
    const requestURL = getRequestURL(ctx.event)
    const { domainLanguage } = getDomainLanguageFromHostname(requestURL.hostname)

    const additionalSitemaps = getAdditionalSitemapUrlsForDomainLanguage(
      domainLanguage,
      requestURL.origin,
      requestURL.host,
    )

    if (!additionalSitemaps.length) {
      return
    }

    const seen = new Set(ctx.sitemaps.map((entry) => entry.sitemap))

    additionalSitemaps.forEach((sitemapUrl) => {
      if (seen.has(sitemapUrl)) {
        return
      }

      ctx.sitemaps.push({ sitemap: sitemapUrl })
      seen.add(sitemapUrl)
    })
  })
}
