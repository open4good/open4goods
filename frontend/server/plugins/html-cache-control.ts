import { applyHtmlCacheHeaders } from '../utils/html-cache-control'

/**
 * Cache-Control of server-rendered pages (GOU-2700): public for anonymous
 * visitors, `private, no-store` for signed-in ones. The edge rule that uses
 * it is described in docs/operations/html-edge-cache.md.
 */
export default defineNitroPlugin(nitroApp => {
  const config = useRuntimeConfig()
  const settings = {
    sMaxAge: Number(config.htmlCache.sMaxAge),
    staleWhileRevalidate: Number(config.htmlCache.staleWhileRevalidate),
    authCookieNames: [
      config.public.tokenCookieName,
      config.public.refreshCookieName,
    ].filter((name): name is string => Boolean(name)),
  }

  nitroApp.hooks.hook('render:response', (response, { event }) => {
    applyHtmlCacheHeaders(event, response, settings)
  })
})
