import type {
  MetrikLatest,
  MetrikMeta,
  MetrikNdjsonRecord,
  MetrikRun,
  MetrikWithTrend,
  MetrikDataPoint,
  MetrikProviderData,
} from '~/types/metriks'

/** Known provider directory names. Add new providers here. */
const PROVIDER_NAMES = ['plausible', 'kibana', 'hello'] as const

const BASE_PATH = '/reports/metriks/data'

/** Supported period presets (in days). 'latest' = compare to the previous run. */
export type MetrikPeriodPreset = 'latest' | '7d' | '3w' | '3m'

/** Maps period preset labels to their approximate day count (0 = previous run). */
export const PERIOD_DAYS: Record<MetrikPeriodPreset, number> = {
  latest: 0,
  '7d': 7,
  '3w': 21,
  '3m': 90,
}

/** Supported chart types. */
export type MetrikChartType = 'bar' | 'line'

/**
 * Parse an NDJSON string into an array of JSON records.
 */
function parseNdjson(text: string): MetrikNdjsonRecord[] {
  return text
    .split('\n')
    .filter(line => line.trim().length > 0)
    .map(line => JSON.parse(line) as MetrikNdjsonRecord)
}

/**
 * Format a metric value for display based on its unit.
 */
export function formatMetrikValue(value: number | null, unit: string): string {
  if (value === null) return '—'

  switch (unit) {
    case 'percent':
      return `${value.toLocaleString()}%`
    case 'ms':
      return value >= 1000 ? `${(value / 1000).toFixed(1)}s` : `${value}ms`
    case 'bytes':
      if (value >= 1_073_741_824)
        return `${(value / 1_073_741_824).toFixed(1)} GB`
      if (value >= 1_048_576) return `${(value / 1_048_576).toFixed(1)} MB`
      if (value >= 1024) return `${(value / 1024).toFixed(1)} KB`
      return `${value} B`
    case 'currency':
      return `${value.toLocaleString()} €`
    case 'ratio':
      return value.toFixed(2)
    case 'count':
      return value.toLocaleString()
    default:
      return value.toLocaleString()
  }
}

/**
 * Get an MDI icon name based on the metric group or id.
 */
export function getMetrikIcon(groups: string[], id: string): string {
  if (groups.includes('analytics')) return 'mdi-chart-line'
  if (groups.includes('goals') && groups.includes('sources'))
    return 'mdi-source-branch'
  if (groups.includes('goals')) return 'mdi-bullseye-arrow'
  if (groups.includes('products')) return 'mdi-package-variant-closed'
  if (groups.includes('observability')) return 'mdi-monitor-eye'
  if (groups.includes('demo')) return 'mdi-hand-wave'

  if (id.includes('visitor')) return 'mdi-account-group'
  if (id.includes('bounce')) return 'mdi-arrow-u-left-top'
  if (id.includes('duration')) return 'mdi-timer-outline'
  if (id.includes('pageview')) return 'mdi-eye'
  if (id.includes('conversion')) return 'mdi-swap-horizontal'

  return 'mdi-chart-box-outline'
}

/**
 * Find the run that is closest to (latestDate - days).
 */
function findComparisonRun(
  runs: MetrikRun[],
  latestRun: MetrikRun,
  days: number
): MetrikRun | null {
  if (runs.length < 2) return null

  const latestTime = new Date(latestRun.period.dateTo).getTime()
  const targetTime = latestTime - days * 24 * 60 * 60 * 1000

  // Find run with dateTo closest to targetTime
  let closestRun: MetrikRun | null = null
  let minDiff = Infinity

  for (const run of runs) {
    if (run === latestRun) continue // Don't compare with self
    const runTime = new Date(run.period.dateTo).getTime()
    // We only want to compare with past runs
    if (runTime > latestTime) continue

    const diff = Math.abs(targetTime - runTime)
    if (diff < minDiff) {
      minDiff = diff
      closestRun = run
    }
  }

  return closestRun
}

/**
 * Composable that loads all metriks data into memory and computes trends.
 *
 * Data is loaded client-side from static JSON/NDJSON files served from
 * the public directory. Supports period-based filtering for trend computation.
 */
export function useMetriks() {
  const providers = ref<Map<string, MetrikProviderData>>(new Map())
  const allRawProviders = ref<Map<string, MetrikProviderData>>(new Map())
  const allMetriks = ref<MetrikWithTrend[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const comparePeriod = ref<MetrikPeriodPreset>('latest')

  /** All unique groups across every metric. */
  const allGroups = computed(() => {
    const groups = new Set<string>()
    for (const m of allMetriks.value) {
      for (const g of m.groups) groups.add(g)
    }
    return Array.from(groups).sort()
  })

  /** All unique tags across every metric. */
  const allTags = computed(() => {
    const tags = new Set<string>()
    for (const m of allMetriks.value) {
      for (const t of m.tags) tags.add(t)
    }
    return Array.from(tags).sort()
  })

  /** All unique provider names. */
  const allProviderNames = computed(() => {
    return Array.from(providers.value.keys()).sort()
  })

  /**
   * Fetch latest.json for a single provider.
   */
  async function fetchLatest(
    providerName: string
  ): Promise<MetrikLatest | null> {
    try {
      const url = `${BASE_PATH}/${providerName}/latest.json`
      const response = await $fetch<MetrikLatest>(url)
      return response
    } catch (e: any) {
      console.warn(
        `[useMetriks] Could not load latest.json for ${providerName}`,
        e
      )
      return null
    }
  }

  /**
   * Fetch events.ndjson for a single provider and parse all runs.
   */
  async function fetchHistory(
    providerName: string
  ): Promise<{ meta: MetrikMeta | null; runs: MetrikRun[] }> {
    try {
      const url = `${BASE_PATH}/${providerName}/events.ndjson`
      const text = await $fetch<string>(url, { responseType: 'text' })
      const records = parseNdjson(text)

      let meta: MetrikMeta | null = null
      const runs: MetrikRun[] = []

      for (const record of records) {
        if (record.type === 'meta') meta = record as MetrikMeta
        else if (record.type === 'run') runs.push(record as MetrikRun)
      }

      return { meta, runs }
    } catch (e: any) {
      console.warn(
        `[useMetriks] Could not load events.ndjson for ${providerName}`,
        e
      )
      return { meta: null, runs: [] }
    }
  }

  /**
   * Build the history data points for a specific event id across runs.
   * Produces a data point for every run date – uses null when the event
   * is absent or its value is null, so the timeline x-axis stays complete.
   */
  function buildHistory(eventId: string, runs: MetrikRun[]): MetrikDataPoint[] {
    const points: MetrikDataPoint[] = runs.map(run => {
      const ev = run.events.find(e => e.id === eventId)
      return {
        date: run.period.dateTo,
        value: ev?.value ?? null,
      }
    })
    return points.sort(
      (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
    )
  }

  /**
   * Compute trend between two values.
   */
  function computeTrend(
    current: number | null,
    previous: number | null
  ): { absoluteChange: number | null; percentChange: number | null } {
    if (current === null || previous === null) {
      return { absoluteChange: null, percentChange: null }
    }
    const absoluteChange = current - previous
    const percentChange =
      previous !== 0 ? (absoluteChange / Math.abs(previous)) * 100 : null
    return { absoluteChange, percentChange }
  }

  /**
   * Build enriched metrics using the latest run for values and
   * a historical run for trend comparison.
   */
  function buildEnrichedMetriks(
    rawProviders: Map<string, MetrikProviderData>,
    days: number
  ): MetrikWithTrend[] {
    const enriched: MetrikWithTrend[] = []

    for (const [providerName, data] of rawProviders) {
      if (data.runs.length === 0) continue

      // 1. Always use the absolute latest run for current values
      const latestRun = data.runs[data.runs.length - 1]!

      // 2. Find comparison run based on selected period
      // days === 0 means "latest" → compare with immediately preceding run
      const compareRun =
        days === 0
          ? data.runs.length >= 2
            ? data.runs[data.runs.length - 2]!
            : null
          : findComparisonRun(data.runs, latestRun, days)

      for (const event of latestRun.events) {
        // Find corresponding event in comparison run
        const previousEvent = compareRun?.events.find(e => e.id === event.id)

        const { absoluteChange, percentChange } = computeTrend(
          event.value,
          previousEvent?.value ?? null
        )

        // 3. History includes ALL runs for the sparkline/chart
        const history = buildHistory(event.id, data.runs)

        enriched.push({
          ...event,
          provider: providerName,
          providerDisplayName: data.meta.providerDisplayName,
          period: latestRun.period, // Current period
          previousValue: previousEvent?.value ?? null,
          absoluteChange,
          percentChange,
          history,
        })
      }
    }

    return enriched
  }

  /**
   * Load all providers and build enriched metrics.
   */
  async function loadAll(): Promise<void> {
    loading.value = true
    error.value = null

    try {
      const providerMap = new Map<string, MetrikProviderData>()

      await Promise.all(
        PROVIDER_NAMES.map(async providerName => {
          const [latest, { meta, runs }] = await Promise.all([
            fetchLatest(providerName),
            fetchHistory(providerName),
          ])

          if (!latest || !meta) return

          providerMap.set(providerName, {
            meta,
            runs,
            latest: latest.run,
          })
        })
      )

      allRawProviders.value = providerMap
      providers.value = providerMap
      recomputeMetriks()
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e)
      console.error('[useMetriks] Failed to load metriks', e)
    } finally {
      loading.value = false
    }
  }

  /**
   * Recompute enriched metrics based on current comparison period.
   */
  function recomputeMetriks(): void {
    const days = PERIOD_DAYS[comparePeriod.value]
    allMetriks.value = buildEnrichedMetriks(allRawProviders.value, days)
  }

  /** Watch period changes to rebuild metrics. */
  watch(comparePeriod, () => {
    if (allRawProviders.value.size > 0) {
      recomputeMetriks()
    }
  })

  return {
    providers,
    allMetriks,
    allGroups,
    allTags,
    allProviderNames,
    loading,
    error,
    comparePeriod, // Export as comparePeriod
    // Actually, let's rename the property in return to match future usage
    // But wait, I should rename the ref too to be clean.
    loadAll,
    formatMetrikValue,
    getMetrikIcon,
  }
}
