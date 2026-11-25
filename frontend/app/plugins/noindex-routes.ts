import { resolveNoindexRobots } from '~~/shared/utils/sitemap-config'

/**
 * Internal tools, developer docs and search results are rendered but kept out of
 * the index (GOU-2696). Done in one place so a new tool page is covered by adding
 * its prefix to NOINDEX_PATH_PREFIXES, which also drops it from the sitemap.
 */
export default defineNuxtPlugin(() => {
  const route = useRoute()

  useHead(() => {
    const robots = resolveNoindexRobots(route.path, route.query)

    return { meta: robots ? [{ name: 'robots', content: robots }] : [] }
  })
})
