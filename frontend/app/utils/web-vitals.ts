/**
 * Helpers of the RUM Core Web Vitals reporting (GOU-2703, audit PERF-17): page template, sampling and the Plausible
 * event payload. Pure functions; the wiring lives in `plugins/web-vitals.client.ts`.
 */

/** Page templates, aligned with the Search Console templates of Metriks (`seo.gsc.template.<id>`). */
export type PageTemplate =
  | 'fiche'
  | 'categorie'
  | 'sous-categorie'
  | 'blog'
  | 'guide'
  | 'opendata'
  | 'autre'

/** Default share of page loads that report their vitals (10 %). */
export const DEFAULT_WEB_VITALS_SAMPLE_RATE = 0.1

const PRODUCT_SEGMENT = /^\d{6,}-.+$/
const CATEGORY_SEGMENT = /^[a-z]+(?:-[a-z]+)*$/

/** Classifies a URL path by the same rules as the Metriks `SeoTemplate` (Java). */
export const resolvePageTemplate = (pathname: string): PageTemplate => {
  const segments = pathname
    .split(/[?#]/)[0]!
    .toLowerCase()
    .split('/')
    .filter(Boolean)

  const [first] = segments
  if (!first) {
    return 'autre'
  }
  if (first === 'blog') {
    return 'blog'
  }
  if (first === 'guides') {
    return 'guide'
  }
  if (first === 'opendata') {
    return 'opendata'
  }
  if (segments.some(segment => PRODUCT_SEGMENT.test(segment))) {
    return 'fiche'
  }
  if (segments.length === 1 && CATEGORY_SEGMENT.test(first)) {
    return 'categorie'
  }
  if (segments.length === 2 && CATEGORY_SEGMENT.test(first)) {
    return 'sous-categorie'
  }
  return 'autre'
}

/** Reads the sample rate (0..1); anything invalid falls back to the default. */
export const normaliseSampleRate = (raw: unknown): number => {
  const rate = typeof raw === 'string' ? Number(raw) : raw
  return typeof rate === 'number' &&
    Number.isFinite(rate) &&
    rate >= 0 &&
    rate <= 1
    ? rate
    : DEFAULT_WEB_VITALS_SAMPLE_RATE
}

/** One decision per page load: `random` is `Math.random()` in production. */
export const isWebVitalsSampled = (rate: number, random: number): boolean =>
  random < rate

/** Subset of the `web-vitals` `Metric` that is reported. */
export interface ReportedMetric {
  name: 'LCP' | 'INP' | 'CLS' | 'TTFB' | 'FCP'
  value: number
  rating: 'good' | 'needs-improvement' | 'poor'
}

/**
 * Plausible props of the `web-vitals` event. `value` is an integer: milliseconds, or CLS x 1000 (a unitless score
 * would otherwise round to 0 and Plausible prop values are strings or integers).
 */
export const buildWebVitalProps = (
  metric: ReportedMetric,
  template: PageTemplate
): {
  metric: string
  template: PageTemplate
  rating: string
  value: number
} => ({
  metric: metric.name,
  template,
  rating: metric.rating,
  value: Math.round(metric.name === 'CLS' ? metric.value * 1000 : metric.value),
})
