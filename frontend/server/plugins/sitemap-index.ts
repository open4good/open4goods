import { access } from 'node:fs/promises'

import { getDomainLanguageFromHostname } from '~~/shared/utils/domain-language'
import { getLocalSitemapFileDescriptorsForDomainLanguage } from '~~/server/utils/sitemap-local-files'

const DEFAULT_SITEMAP_ORIGIN = 'https://nudger.fr'

type RequestOriginEvent = {
  node?: {
    req?: {
      headers?: Record<string, string | string[] | undefined>
    }
  }
}

const getFallbackRequestUrl = () =>
  new URL(
    process.env.NUXT_PUBLIC_SITE_URL ||
      process.env.NUXT_SITE_URL ||
      DEFAULT_SITEMAP_ORIGIN
  )

const getHeaderValue = (value: string | string[] | undefined) =>
  Array.isArray(value) ? value[0] : value

const getSafeRequestUrl = (event: RequestOriginEvent | undefined) => {
  const headers = event?.node?.req?.headers
  const forwardedHost = getHeaderValue(headers?.['x-forwarded-host'])
  const host = forwardedHost ?? getHeaderValue(headers?.host)
  const forwardedProtocol = getHeaderValue(headers?.['x-forwarded-proto'])
  const protocol = forwardedProtocol?.split(',')[0]?.trim() || 'https'

  return host ? new URL(`${protocol}://${host}`) : getFallbackRequestUrl()
}

export default (nitroApp: import('nitro/app').NitroApp) => {
  nitroApp.hooks.hook('sitemap:index-resolved', async ctx => {
    let requestURL = getSafeRequestUrl(ctx.event)

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

    const descriptors =
      getLocalSitemapFileDescriptorsForDomainLanguage(domainLanguage)

    if (!descriptors.length) {
      return
    }

    const seen = new Set(ctx.sitemaps.map(entry => entry.sitemap))

    for (const descriptor of descriptors) {
      // Skip sitemaps whose backing file doesn't exist yet (e.g. guides.xml before first generation run)
      try {
        await access(descriptor.filePath)
      } catch {
        continue
      }

      let sitemapUrl: string
      try {
        sitemapUrl = new URL(
          descriptor.publicPath,
          requestURL.origin
        ).toString()
      } catch {
        continue
      }

      if (seen.has(sitemapUrl)) {
        continue
      }

      ctx.sitemaps.push({ sitemap: sitemapUrl })
      seen.add(sitemapUrl)
    }
  })
}
