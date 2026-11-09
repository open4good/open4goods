import type {
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
