import { computed } from 'vue'
import type { Ref } from 'vue'
import { mdiLeaf, mdiPodium } from '@mdi/js'
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
import {
  isImpactScoreNotRated,
  resolveImpactScoreV2,
  resolvePositionLevel,
} from '../utils/_impact-score-v2'

// V2 colours follow the position level, never fixed score thresholds.
const V2_LEVEL_COLORS = {
  excellent: 'success',
  good: 'success',
  fair: 'warning',
  poor: 'warning',
} as const

export function useProductVerdict(
  productRef: Ref<ProductDto | null | undefined>
) {
  const impactVerdict = computed<VerdictDimension | null>(() => {
    const product = productRef.value
    if (!product) return null

    const score = resolvePrimaryImpactScore(product)
    const v2 = resolveImpactScoreV2(product.scores)
    if (v2) {
      const positionLevel = resolvePositionLevel(v2.level)
      if (isImpactScoreNotRated(v2) || score === null || !positionLevel) {
        return {
          level: 'insufficient',
          score: null,
          scoreOutOf: 20,
          labelKey: 'components.impactScore.v2.notRated',
          color: 'grey',
          icon: mdiPodium,
        }
      }
      return {
        level: positionLevel,
        score,
        scoreOutOf: 20,
        labelKey: `components.impactScore.v2.positionLevels.${positionLevel}`,
        color: V2_LEVEL_COLORS[positionLevel],
        icon: mdiPodium,
      }
    }
    if (score === null) {
      return {
        level: 'insufficient',
        score: null,
        scoreOutOf: 20,
        labelKey: 'product.verdict.levels.unrated',
        color: 'grey',
        icon: mdiLeaf,
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
      icon: mdiLeaf,
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
