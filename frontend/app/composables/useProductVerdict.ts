import { computed } from 'vue'
import type { Ref } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import {
  getImpactLevel,
  getImpactColor,
  getPriceVerdict,
  getReliabilityVerdict,
  type VerdictDimension,
  type PriceVerdict,
} from '../utils/_product-verdict'
import { resolvePrimaryImpactScore } from '../utils/_product-scores'

export function useProductVerdict(
  productRef: Ref<ProductDto | null | undefined>
) {
  const impactVerdict = computed<VerdictDimension | null>(() => {
    const product = productRef.value
    if (!product) return null

    const score = resolvePrimaryImpactScore(product)
    if (score === null) {
      return {
        level: 'insufficient',
        score: null,
        scoreOutOf: 20,
        labelKey: 'product.verdict.levels.insufficient',
        color: 'grey',
        icon: 'mdi-leaf-polyline',
      }
    }

    const level = getImpactLevel(score)
    const color = getImpactColor(level)

    return {
      level,
      score,
      scoreOutOf: 20,
      labelKey: `product.verdict.levels.${level}`,
      color,
      icon: 'mdi-leaf',
    }
  })

  const priceVerdict = computed<PriceVerdict | null>(() => {
    const product = productRef.value
    if (!product) return null
    return getPriceVerdict(product)
  })

  const reliabilityVerdict = computed<VerdictDimension | null>(() => {
    const product = productRef.value
    if (!product) return null
    return getReliabilityVerdict(product)
  })

  const hasVerdict = computed(() => {
    return !!productRef.value
  })

  return {
    impactVerdict,
    priceVerdict,
    reliabilityVerdict,
    hasVerdict,
  }
}
