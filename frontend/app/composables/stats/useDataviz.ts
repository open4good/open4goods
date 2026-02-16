import type {
  DatavizChartQueryRequestDto,
  DatavizChartQueryResponseDto,
  DatavizHeroStatsDto,
  VerticalDatavizPlanDto,
} from '~~/shared/api-client'

/**
 * Composable for dataviz statistics dashboard.
 * Fetches plan, hero KPIs, and chart data from the Nuxt server routes.
 */
export const useDataviz = (verticalId: string) => {
  const plan = useState<VerticalDatavizPlanDto | null>(
    `dataviz-plan-${verticalId}`,
    () => null
  )
  const heroStats = useState<DatavizHeroStatsDto | null>(
    `dataviz-hero-${verticalId}`,
    () => null
  )
  const planLoading = useState(
    `dataviz-plan-loading-${verticalId}`,
    () => false
  )
  const heroLoading = useState(
    `dataviz-hero-loading-${verticalId}`,
    () => false
  )
  const error = useState<string | null>(
    `dataviz-error-${verticalId}`,
    () => null
  )

  const fetchPlan = async (): Promise<VerticalDatavizPlanDto | null> => {
    if (plan.value) {
      return plan.value
    }

    planLoading.value = true
    error.value = null

    try {
      plan.value = await $fetch<VerticalDatavizPlanDto>(
        '/api/stats/dataviz/plan',
        {
          params: { verticalId },
        }
      )
      return plan.value
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch dataviz plan'
      console.error('Error in fetchPlan:', err)
      return null
    } finally {
      planLoading.value = false
    }
  }

  const fetchHeroStats = async (): Promise<DatavizHeroStatsDto | null> => {
    if (heroStats.value) {
      return heroStats.value
    }

    heroLoading.value = true
    error.value = null

    try {
      heroStats.value = await $fetch<DatavizHeroStatsDto>(
        '/api/stats/dataviz/hero',
        {
          params: { verticalId },
        }
      )
      return heroStats.value
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch hero stats'
      console.error('Error in fetchHeroStats:', err)
      return null
    } finally {
      heroLoading.value = false
    }
  }

  const fetchChartData = async (
    request: DatavizChartQueryRequestDto
  ): Promise<DatavizChartQueryResponseDto | null> => {
    try {
      return await $fetch<DatavizChartQueryResponseDto>(
        '/api/stats/dataviz/chart',
        {
          method: 'POST',
          params: { verticalId },
          body: request,
        }
      )
    } catch (err) {
      console.error('Error in fetchChartData:', err)
      return null
    }
  }

  return {
    plan: readonly(plan),
    heroStats: readonly(heroStats),
    planLoading: readonly(planLoading),
    heroLoading: readonly(heroLoading),
    error: readonly(error),

    fetchPlan,
    fetchHeroStats,
    fetchChartData,
  }
}
