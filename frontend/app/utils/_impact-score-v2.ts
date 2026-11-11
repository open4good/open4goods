import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'
import type {
  ProductDto,
  ProductImpactScoreStatusDto,
  ProductScoresDto,
} from '~~/shared/api-client'

/** First methodology version whose `impactScore` block the frontend displays. */
export const IMPACT_SCORE_V2_MIN_VERSION = 2

export type ImpactScorePositionLevel = 'excellent' | 'good' | 'fair' | 'poor'

const POSITION_LEVELS: readonly ImpactScorePositionLevel[] = [
  'excellent',
  'good',
  'fair',
  'poor',
]

export type ImpactScoreNotRatedReasonKey = 'coverage' | 'recalculating'

/**
 * Returns the `impactScore` status block only when the vertical runs the
 * method V2 or later. In V1 it returns null: the caller keeps the historical
 * rendering untouched. The version rule itself lives in the front-api.
 */
export const resolveImpactScoreV2 = (
  scores?: ProductScoresDto | null
): ProductImpactScoreStatusDto | null => {
  const status = scores?.impactScore
  if (!status) return null
  if ((status.currentMethodologyVersion ?? 1) < IMPACT_SCORE_V2_MIN_VERSION) {
    return null
  }
  return status
}

export const isImpactScoreNotRated = (
  status: ProductImpactScoreStatusDto | null | undefined
): boolean => status?.status === 'NOT_RATED'

export const resolvePositionLevel = (
  level?: string | null
): ImpactScorePositionLevel | null =>
  POSITION_LEVELS.find(candidate => candidate === level) ?? null

export const resolveNotRatedReasonKey = (
  status: ProductImpactScoreStatusDto
): ImpactScoreNotRatedReasonKey =>
  status.notRatedReason === 'LOW_COVERAGE' ||
  status.notRatedReason === 'NO_REAL_CRITERIA'
    ? 'coverage'
    : 'recalculating'

/** Position in percent, only when the rated population allows a comparison. */
export const resolveBetterThanPercent = (
  status: ProductImpactScoreStatusDto
): number | null => {
  const percent = status.betterThanPercent
  if (typeof percent !== 'number' || !Number.isFinite(percent)) return null
  if ((status.ratedCount ?? 0) <= 1) return null
  return Math.max(0, Math.min(Math.round(percent), 100))
}

export interface ImpactScoreCriteriaCount {
  count: number
  total: number
}

/** "x criteria out of y filled in", only when both counts are known. */
export const resolveCriteriaCount = (
  status: ProductImpactScoreStatusDto | null | undefined
): ImpactScoreCriteriaCount | null => {
  const count = status?.realCriteriaCount
  const total = status?.totalCriteriaCount
  if (typeof count !== 'number' || typeof total !== 'number') return null
  return { count, total }
}

/**
 * Whether the compared V2 products rest on different criteria. The API only
 * exposes counts, so two products differ when their counts or totals differ.
 * Products without a V2 block (V1) or without counts are ignored.
 */
export const haveDifferentCriteriaSets = (
  statuses: ReadonlyArray<ProductImpactScoreStatusDto | null | undefined>
): boolean => {
  const counts = statuses
    .map(resolveCriteriaCount)
    .filter((c): c is ImpactScoreCriteriaCount => c !== null)
  if (counts.length < 2) return false
  const [first] = counts
  return counts.some(c => c.count !== first!.count || c.total !== first!.total)
}

/**
 * The front-api already lists the not rated products last: the page only
 * says so when the active sort is the Impact Score on a V2 vertical.
 */
export const shouldShowNotRatedAfterRatedNotice = (
  sortField: string | null | undefined,
  products: ReadonlyArray<ProductDto>
): boolean =>
  sortField === ECOSCORE_RELATIVE_FIELD &&
  products.some(product => resolveImpactScoreV2(product.scores) !== null)
