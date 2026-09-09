/**
 * Response shape for the Nuxt-internal `/api/pages/[pageId]` endpoint. All full pages are now
 * served from `server/utils/static-full-pages.ts` -- there is no live XWiki backend behind this
 * type any more (xwiki-editorial-content-to-nuxt-content AC2).
 */
export interface CmsFullPage {
  htmlContent: string
  pageTitle?: string | null
  metaTitle?: string | null
  metaDescription?: string | null
  width?: string | null
  editLink?: string | null
}
