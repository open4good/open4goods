import { computed } from 'vue'
import type { Ref } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import { normalizeTimestamp } from '~/utils/date-parsing'

export interface VigilanceAlert {
  key: 'eol' | 'conflicts' | 'quality' | 'obsolescence' | 'competition'
  titleKey: string
  icon: string
  color: string
}

export function useProductVigilance(
  productRef: Ref<ProductDto | null | undefined>
) {
  const isEndOfLife = computed(() => {
    const product = productRef.value
    if (!product) return false
    const onMarketEndDate = product.eprel?.onMarketEndDate
    if (!onMarketEndDate) return false
    const normalized = normalizeTimestamp(onMarketEndDate)
    if (!normalized) return false
    const date = new Date(normalized)
    if (isNaN(date.getTime())) return false
    return date < new Date()
  })

  const hasConflictingAttributes = computed(() => {
    const product = productRef.value
    if (!product) return false
    const allAttributes = product.attributes?.allAttributes ?? {}
    return Object.values(allAttributes).some(
      attr => attr.sourcing?.conflicts === true
    )
  })

  const isLowDataQuality = computed(() => {
    const product = productRef.value
    if (!product) return false
    const dqScore = product.scores?.scores?.['DATA_QUALITY']
    if (!dqScore) return false
    const val = (dqScore.value ?? 0) * 4
    const avg = (dqScore.relativ?.avg ?? dqScore.absolute?.avg ?? 0) * 4
    return val < avg
  })

  const hasObsolescenceWarning = computed(() => {
    const product = productRef.value
    if (!product) return false
    const warning = product.aiReview?.review?.obsolescenceWarning
    return !!warning && warning.length > 0
  })

  const isLowCompetition = computed(() => {
    const product = productRef.value
    if (!product) return false
    const byCondition = product.offers?.offersByCondition ?? {}
    let count = 0
    for (const offers of Object.values(byCondition)) {
      if (Array.isArray(offers)) {
        count += offers.length
      }
    }
    return count > 0 && count <= 2
  })

  const alerts = computed<VigilanceAlert[]>(() => {
    const list: VigilanceAlert[] = []
    if (isEndOfLife.value) {
      list.push({
        key: 'eol',
        titleKey: 'product.impact.endOfLifeTitle',
        icon: 'mdi-alert-decagram-outline',
        color: 'warning',
      })
    }
    if (hasConflictingAttributes.value) {
      list.push({
        key: 'conflicts',
        titleKey: 'product.vigilance.conflicts.title',
        icon: 'mdi-alert-circle-outline',
        color: 'error',
      })
    }
    if (isLowDataQuality.value) {
      list.push({
        key: 'quality',
        titleKey: 'product.vigilance.quality.title',
        icon: 'mdi-database-alert-outline',
        color: 'warning',
      })
    }
    if (hasObsolescenceWarning.value) {
      list.push({
        key: 'obsolescence',
        titleKey: 'product.vigilance.obsolescence.title',
        icon: 'mdi-timer-alert-outline',
        color: 'warning',
      })
    }
    if (isLowCompetition.value) {
      list.push({
        key: 'competition',
        titleKey: 'product.price.competition.title',
        icon: 'mdi-alert-outline',
        color: 'warning',
      })
    }
    return list
  })

  const hasAlerts = computed(() => alerts.value.length > 0)

  return {
    isEndOfLife,
    hasConflictingAttributes,
    isLowDataQuality,
    hasObsolescenceWarning,
    isLowCompetition,
    alerts,
    hasAlerts,
  }
}
