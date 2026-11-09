import type { ProductImpactScoreStatusDto } from '~~/shared/api-client'
import type { ScoreView } from '~/components/product/impact/impact-types'
import {
  isImpactScoreNotRated,
  resolvePositionLevel,
} from '~/utils/_impact-score-v2'

/** Midpoint of the 0-5 relative scale, normalised on the category distribution. */
export const RELATIVE_SCALE_MIDPOINT = 2.5

/** Number of strong / weak criteria cited at most. */
export const WHY_CITED_CRITERIA = 2

export type WhyCriterion = {
  id: string
  label: string
  /** Raw value with its unit, null when the product has none to display. */
  value: string | null
  /** Category average with its unit, null when unavailable. */
  categoryAverage: string | null
  /** Relative score minus the scale midpoint (positive = above average). */
  deviation: number
}

export type WhyHighlights = {
  strengths: WhyCriterion[]
  weaknesses: WhyCriterion[]
  missing: string[]
}

export type WhyThisScoreVariant = 'top' | 'middle' | 'bottom' | 'notRated'

const isFiniteNumber = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value)

const formatValue = (
  value: string | number | null | undefined,
  suffix: string | null | undefined
): string | null => {
  if (value == null) return null
  const text = String(value).trim()
  if (!text.length) return null
  const unit = suffix?.trim()
  return unit && !text.endsWith(unit) ? `${text} ${unit}` : text
}

const formatAverage = (score: ScoreView): string | null => {
  const avg = score.absolute?.avg
  if (!isFiniteNumber(avg)) return null
  const rounded = Math.round(avg * 10) / 10
  return formatValue(rounded, score.attributeSuffix ?? score.unit)
}

const compareByDeviation =
  (direction: 1 | -1) =>
  (
    a: WhyCriterion & { weight: number },
    b: WhyCriterion & { weight: number }
  ) =>
    direction * (b.deviation - a.deviation) ||
    b.weight - a.weight ||
    a.id.localeCompare(b.id)

/**
 * Picks the criteria that move the furthest from the category average, up or
 * down. Only filled criteria are ever cited as strengths or weaknesses; the
 * matrix criteria the product does not have are listed apart as missing, never
 * as weak points. Ordering is deterministic (deviation, then weight, then id).
 */
export const computeWhyHighlights = (
  scores: readonly ScoreView[],
  limit: number = WHY_CITED_CRITERIA
): WhyHighlights => {
  const filled = scores
    .filter(
      score =>
        !score.notFilled &&
        !score.virtual &&
        score.id?.toUpperCase() !== 'ECOSCORE' &&
        isFiniteNumber(score.relativeValue)
    )
    .map(score => ({
      id: String(score.id),
      label: score.label,
      value: formatValue(score.attributeValue, score.attributeSuffix),
      categoryAverage: formatAverage(score),
      deviation: (score.relativeValue as number) - RELATIVE_SCALE_MIDPOINT,
      weight: isFiniteNumber(score.coefficient) ? score.coefficient : 0,
    }))

  const strip = ({ weight: _weight, ...criterion }: (typeof filled)[number]) =>
    criterion

  return {
    strengths: filled
      .filter(item => item.deviation > 0)
      .sort(compareByDeviation(1))
      .slice(0, limit)
      .map(strip),
    weaknesses: filled
      .filter(item => item.deviation < 0)
      .sort(compareByDeviation(-1))
      .slice(0, limit)
      .map(strip),
    missing: scores
      .filter(score => score.notFilled)
      .sort(
        (a, b) =>
          (b.coefficient ?? 0) - (a.coefficient ?? 0) ||
          String(a.id).localeCompare(String(b.id))
      )
      .map(score => score.label),
  }
}

/** Variant of the block. Thresholds come from the position level (front-api). */
export const resolveWhyVariant = (
  status: ProductImpactScoreStatusDto
): WhyThisScoreVariant => {
  if (isImpactScoreNotRated(status)) return 'notRated'
  const level = resolvePositionLevel(status.level)
  if (level === 'excellent') return 'top'
  if (level === 'poor') return 'bottom'
  return 'middle'
}
