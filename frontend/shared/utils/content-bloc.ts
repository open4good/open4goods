/**
 * Response shape for the Nuxt-internal `/api/blocs/[blocId]` endpoint. All content blocs are now
 * served from `server/utils/static-content-blocs.ts` -- there is no live backend behind this type
 * any more (xwiki-editorial-content-to-nuxt-content AC2).
 */
export interface ContentBloc {
  blocId?: string
  htmlContent?: string
  editLink?: string | null
}
