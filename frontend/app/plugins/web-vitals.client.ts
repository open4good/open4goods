import { defineNuxtPlugin, useRuntimeConfig } from '#imports'
import { useAnalytics } from '~/composables/useAnalytics'
import {
  buildWebVitalProps,
  isWebVitalsSampled,
  normaliseSampleRate,
  resolvePageTemplate,
  type ReportedMetric,
} from '~/utils/web-vitals'

/**
 * Real-user Core Web Vitals (LCP, INP, CLS, TTFB) sent as `web-vitals` Plausible events with the page template as a
 * prop (GOU-2703). A page load is sampled once (`runtimeConfig.public.webVitalsSampleRate`, default 10 %). Events go
 * through `useAnalytics().trackEvent`, so the Plausible privacy guard (DNT, `/contrib/`, local referrer, automated
 * browsers, `enabled`) applies unchanged. The library is loaded after mount to stay out of the critical path.
 */
export default defineNuxtPlugin({
  name: 'web-vitals',
  setup(nuxtApp) {
    const rate = normaliseSampleRate(
      useRuntimeConfig().public.webVitalsSampleRate
    )
    if (!isWebVitalsSampled(rate, Math.random())) {
      return
    }

    nuxtApp.hook('app:mounted', async () => {
      const { trackEvent, isAnalyticsEnabled } = useAnalytics()
      if (!isAnalyticsEnabled()) {
        return
      }
      // Hard navigations only: the landing page is the page the metrics belong to.
      const template = resolvePageTemplate(window.location.pathname)
      const report = (metric: ReportedMetric) =>
        trackEvent('web-vitals', {
          props: buildWebVitalProps(metric, template),
        })

      try {
        const { onCLS, onINP, onLCP, onTTFB } = await import('web-vitals')
        onLCP(report)
        onINP(report)
        onCLS(report)
        onTTFB(report)
      } catch (error) {
        console.warn('web-vitals unavailable', error)
      }
    })
  },
})
