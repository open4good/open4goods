import { getDomainLanguageFromHostname } from '~~/shared/utils/domain-language'
import { APP_ROUTES_SITEMAP_KEY } from '~~/shared/utils/sitemap-config'
import { useOpenDataService } from '~~/shared/api-client/services/opendata.services'

const APP_ROUTES_SITEMAP_FILENAME = `${APP_ROUTES_SITEMAP_KEY}.xml`

/**
 * Adds `/opendata/impact/{vertical}` to the app routes sitemap (GOU-2251).
 *
 * The category pages are a dynamic route (`opendata/impact/[vertical].vue`), absent from the static page scan: their
 * list comes from the impact datasets of the published manifest. When front-api cannot be read the sitemap is served
 * without them rather than failing.
 */
export default defineNitroPlugin(nitroApp => {
  nitroApp.hooks.hook('sitemap:sources', async ctx => {
    if (
      ctx.sitemapName !== APP_ROUTES_SITEMAP_FILENAME &&
      ctx.sitemapName !== APP_ROUTES_SITEMAP_KEY
    ) {
      return
    }

    const headers = ctx.event?.node?.req?.headers
    const forwardedHost = headers?.['x-forwarded-host']
    const host =
      (Array.isArray(forwardedHost) ? forwardedHost[0] : forwardedHost) ??
      headers?.host ??
      'nudger.fr'
    const { domainLanguage } = getDomainLanguageFromHostname(
      host.split(':')[0] ?? 'nudger.fr'
    )

    try {
      const datasets =
        await useOpenDataService(domainLanguage).fetchImpactDatasets()
      const urls = datasets
        .map(dataset => (dataset.id ?? '').replace(/^impact-/, ''))
        .filter(vertical => vertical.length > 0)
        .map(vertical => ({ loc: `/opendata/impact/${vertical}` }))

      if (!urls.length) {
        return
      }

      ctx.sources.push({
        context: {
          name: 'opendata-impact-categories',
          description: 'Impact dataset pages listed in the published manifest',
        },
        sourceType: 'user',
        urls,
      })
    } catch (error) {
      console.error('Sitemap: impact datasets unavailable', error)
    }
  })
})
