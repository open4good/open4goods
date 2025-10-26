import { getRequestURL } from 'h3'
import type { SitemapUrlInput } from '@nuxtjs/sitemap'

import { APP_ROUTES_SITEMAP_KEY } from '~~/shared/utils/sitemap-config'
import { getDomainLanguageFromHostname } from '~~/shared/utils/domain-language'
import { getMainPagePathsForDomainLanguage } from '~~/shared/utils/sitemap-main-pages'

const MAIN_PAGES_SITEMAP_FILENAME = `${APP_ROUTES_SITEMAP_KEY}.xml`

const extractUrlKey = (entry: SitemapUrlInput): string | null => {
  if (typeof entry === 'string') {
    return entry
  }

  if (!entry || typeof entry !== 'object') {
    return null
  }

  if ('loc' in entry && typeof entry.loc === 'string' && entry.loc) {
    return entry.loc
  }

  if ('url' in entry && typeof (entry as { url?: unknown }).url === 'string') {
    const url = (entry as { url?: string }).url

    return url && url.length > 0 ? url : null
  }

  return null
}

export default defineNitroPlugin((nitroApp) => {
  nitroApp.hooks.hook('sitemap:input', (ctx) => {
    if (ctx.sitemapName !== MAIN_PAGES_SITEMAP_FILENAME) {
      return
    }

    const requestURL = ctx.event ? getRequestURL(ctx.event) : new URL('https://nudger.com')
    const { domainLanguage } = getDomainLanguageFromHostname(requestURL.hostname)

    const staticPaths = getMainPagePathsForDomainLanguage(domainLanguage)

    if (!staticPaths.length) {
      return
    }

    const seen = new Set<string>()

    ctx.urls.forEach((entry) => {
      const key = extractUrlKey(entry)

      if (!key) {
        return
      }

      seen.add(key)
    })

    staticPaths.forEach((path) => {
      if (seen.has(path)) {
        return
      }

      ctx.urls.push({ loc: path })
      seen.add(path)
    })
  })
})
