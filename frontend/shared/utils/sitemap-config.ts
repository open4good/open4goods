export const SITEMAP_PATH_PREFIX = '/sitemap'
export const APP_ROUTES_SITEMAP_KEY = 'main-pages'
export const BLOG_POSTS_SITEMAP_KEY = 'blog-posts'

/**
 * Internal tools and developer docs: kept out of the index (`noindex, follow`)
 * and out of the main-pages sitemap. Matches the path itself and its sub-paths.
 */
export const NOINDEX_PATH_PREFIXES = [
  '/metriks',
  '/prompt',
  '/docs-browser',
  '/comparer',
  '/docs',
  '/auth',
  '/share',
  '/admin',
  '/cms-fullpage',
  '/offline',
] as const

/** Search results: `/rechercher` is indexable bare, never with a query (infinite URL space). */
export const SEARCH_PATH = '/rechercher'

const matchesPrefix = (path: string, prefix: string): boolean =>
  path === prefix || path.startsWith(`${prefix}/`)

export const isNoindexPath = (path: string): boolean =>
  NOINDEX_PATH_PREFIXES.some(prefix => matchesPrefix(path, prefix))

/** Robots directive for a request, or `null` when the page may be indexed. */
export const resolveNoindexRobots = (
  path: string,
  query: Record<string, unknown> = {}
): string | null => {
  if (isNoindexPath(path)) {
    return 'noindex, follow'
  }

  if (path === SEARCH_PATH && Object.keys(query).length > 0) {
    return 'noindex, follow'
  }

  return null
}
