/**
 * Pages declared with `definePageMeta({ htmlCache: false })` render per-visitor
 * content: flag the request so the Nitro render hook keeps their HTML out of
 * shared caches (server/plugins/html-cache-control.ts, GOU-2700).
 */
export default defineNuxtPlugin(nuxtApp => {
  const router = useRouter()

  nuxtApp.hook('app:rendered', () => {
    const event = nuxtApp.ssrContext?.event
    if (event && router.currentRoute.value.meta.htmlCache === false) {
      event.context.htmlCachePrivate = true
    }
  })
})
