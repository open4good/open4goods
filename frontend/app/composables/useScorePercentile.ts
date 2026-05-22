import { computed } from 'vue'
import type { Ref } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import { getImpactLevel } from '../utils/_product-verdict'

export function useScorePercentile(
  productRef: Ref<ProductDto | null | undefined>
) {
  const score = computed(
    () => productRef.value?.scores?.scores?.ECOSCORE?.value ?? null
  )

  const qualitativeLabelKey = computed(() => {
    if (score.value === null) return 'product.verdict.levels.insufficient'
    const level = getImpactLevel(score.value)
    return `product.verdict.levels.${level}`
  })

  const ranking = computed(
    () => productRef.value?.scores?.scores?.ECOSCORE?.ranking ?? null
  )
  const count = computed(
    () => productRef.value?.scores?.scores?.ECOSCORE?.absolute?.count ?? null
  )

  const percentile = computed(() => {
    if (ranking.value === null || count.value === null || count.value <= 1) {
      return null
    }
    // If rank is 1 (best) and count is 100, then (100 - 1) / 100 = 99% (better than 99% of category)
    const pct = Math.round(((count.value - ranking.value) / count.value) * 100)
    return Math.max(0, Math.min(pct, 100))
  })

  const hasPercentile = computed(() => percentile.value !== null)

  return {
    score,
    qualitativeLabelKey,
    percentile,
    hasPercentile,
    ranking,
    count,
  }
}
