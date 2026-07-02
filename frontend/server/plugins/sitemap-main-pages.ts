import { APP_ROUTES_SITEMAP_KEY } from '~~/shared/utils/sitemap-config'
import { getDomainLanguageFromHostname } from '~~/shared/utils/domain-language'
import { getMainPagePathsForDomainLanguage } from '~~/shared/utils/sitemap-main-pages'

const MAIN_PAGES_SITEMAP_FILENAME = `${APP_ROUTES_SITEMAP_KEY}.xml`
const DEFAULT_SITEMAP_ORIGIN = 'https://nudger.fr'

type RequestOriginEvent = {
  node?: {
    req?: {
      headers?: Record<string, string | string[] | undefined>
    }
  }
}

const getHeaderValue = (value: string | string[] | undefined) =>
  Array.isArray(value) ? value[0] : value

const getRequestOriginUrl = (event: RequestOriginEvent | undefined) => {
  const headers = event?.node?.req?.headers
  const forwardedHost = getHeaderValue(headers?.['x-forwarded-host'])
  const host = forwardedHost ?? getHeaderValue(headers?.host)
  const forwardedProtocol = getHeaderValue(headers?.['x-forwarded-proto'])
  const protocol = forwardedProtocol?.split(',')[0]?.trim() || 'https'

  return host
    ? new URL(`${protocol}://${host}`)
    : new URL(DEFAULT_SITEMAP_ORIGIN)
}

export default defineNitroPlugin(nitroApp => {
  nitroApp.hooks.hook('sitemap:sources', ctx => {
    if (
      ctx.sitemapName !== MAIN_PAGES_SITEMAP_FILENAME &&
      ctx.sitemapName !== APP_ROUTES_SITEMAP_KEY
    ) {
      return
    }

    let requestURL = getRequestOriginUrl(ctx.event)

    // Fallback for static generation where requestURL might be localhost
    if (
      (process.env.NODE_ENV === 'production' ||
        process.env.NODE_ENV === 'prerender') &&
      (requestURL.hostname === 'localhost' ||
        requestURL.hostname === '127.0.0.1')
    ) {
      requestURL = new URL('https://nudger.fr')
    }

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
