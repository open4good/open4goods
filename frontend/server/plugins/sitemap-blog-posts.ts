import { getDomainLanguageFromHostname } from '~~/shared/utils/domain-language'
import { BLOG_POSTS_SITEMAP_KEY } from '~~/shared/utils/sitemap-config'
import { listBlogDocs, blogDocToSitemapUrl } from '~~/server/utils/blog-content'

const BLOG_POSTS_SITEMAP_FILENAME = `${BLOG_POSTS_SITEMAP_KEY}.xml`

/**
 * Registers the blog article sitemap from the `blog` Nuxt Content collection.
 *
 * Blog articles are dynamic routes (app/pages/blog/[slug].vue), so they are excluded from
 * shared/utils/sitemap-main-pages.ts's static-page scan and had no sitemap source at all: the
 * `blog-posts.xml` name reserved in nuxt.config.ts's sitemapLocalFiles was never written by ui's
 * SitemapGenerationService (it only ever produced product-pages.xml, wiki-pages.xml,
 * category-pages.xml and guides.xml), so that proxy entry always 404'd. This plugin makes Nuxt the
 * actual source of truth for blog URLs, consistent with rss.get.ts's own reasoning: "Nuxt -- not
 * the Java UI module -- owns the blog." See xwiki-editorial-content-to-nuxt-content AC3.
 *
 * Content is French-only today (content/blog/fr/**), so only the 'fr' domain gets entries.
 */
export default (nitroApp: import('nitro/app').NitroApp) => {
  nitroApp.hooks.hook('sitemap:sources', async ctx => {
    if (ctx.sitemapName !== BLOG_POSTS_SITEMAP_FILENAME) {
      return
    }

    const headers = ctx.event?.node?.req?.headers
    const forwardedHost = headers?.['x-forwarded-host']
    const host = (Array.isArray(forwardedHost) ? forwardedHost[0] : forwardedHost) ?? headers?.host
    const hostname = (Array.isArray(host) ? host[0] : host) ?? 'nudger.fr'
    const { domainLanguage } = getDomainLanguageFromHostname(hostname)

    if (domainLanguage !== 'fr') {
      return
    }

    const docs = await listBlogDocs(ctx.event)

    ctx.sources.push({
      context: {
        name: 'blog-posts',
        description: 'Blog articles from the Nuxt Content collection',
      },
      sourceType: 'user',
      urls: docs.map(blogDocToSitemapUrl),
    })
  })
}
