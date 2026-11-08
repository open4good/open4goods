/**
 * Last blog article or guide displayed in this tab during this SPA session
 * (GOU-1829). In-memory Nuxt state only: no cookie, no web storage. It is
 * only written client-side, so SSR HTML never carries it.
 */
export const useAffiliationContentPath = () =>
  useState<string | null>('affiliation-content-path', () => null)
