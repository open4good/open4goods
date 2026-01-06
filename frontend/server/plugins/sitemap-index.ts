import { getRequestURL } from 'h3'

import { getDomainLanguageFromHostname } from '~~/shared/utils/domain-language'
import { getPublicSitemapUrlsForDomainLanguage } from '~~/server/utils/sitemap-local-files'

export default (nitroApp: import('nitro/app').NitroApp) => {
  nitroApp.hooks.hook('sitemap:index-resolved', ctx => {
    let requestURL = getRequestURL(ctx.event)

    // Fallback for static generation where requestURL might be localhost (NODE_ENV is 'prerender' during generation)
    if (
      (process.env.NODE_ENV === 'production' ||
        process.env.NODE_ENV === 'prerender') &&
      (requestURL.hostname === 'localhost' ||
        requestURL.hostname === '127.0.0.1')
    ) {
      // Default to main production domain if we can't determine otherwise during generation
      // ideally this should be driven by NUXT_PUBLIC_SITE_URL or similar but for now we fallback to default
      requestURL = new URL('https://nudger.fr')
    }

    const { domainLanguage } = getDomainLanguageFromHostname(
      requestURL.hostname
    )

    const additionalSitemaps = getPublicSitemapUrlsForDomainLanguage(
      domainLanguage,
      requestURL.origin
    )

    if (!additionalSitemaps.length) {
      return
    }

    const seen = new Set(ctx.sitemaps.map(entry => entry.sitemap))

    additionalSitemaps.forEach(sitemapUrl => {
      if (seen.has(sitemapUrl)) {
        return
      }

      ctx.sitemaps.push({ sitemap: sitemapUrl })
      seen.add(sitemapUrl)
    })
  })
}
