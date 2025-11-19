import type { ProductDto } from '~~/shared/api-client'

const clampScore = (score: number) => Math.max(0, Math.min(score, 5))
const isFiniteNumber = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value)

/**
 * Resolve the primary impact score for a product on a five-point scale.
 * Falls back to any score whose identifier mentions "impact" when the
 * ECOSCORE entry is missing.
 */
export const resolvePrimaryImpactScore = (product: ProductDto): number | null => {
  const scores = product.scores?.scores

  if (!scores) {
    return null
  }

  const preferredKeys = ['ECOSCORE']

  const impactEntry =
    preferredKeys.map((key) => scores[key]).find((entry) => entry != null) ??
    Object.values(scores).find((entry) => entry?.id?.toLowerCase()?.includes('impact')) ??
    null

  if (!impactEntry) {
    return null
  }

  if (isFiniteNumber(impactEntry.on20)) {
    return clampScore((impactEntry.on20 / 20) * 5)
  }

  if (isFiniteNumber(impactEntry.percent)) {
    return clampScore((impactEntry.percent / 100) * 5)
  }

  const absoluteValue = impactEntry.absolute?.value
  const absoluteMax = impactEntry.absolute?.max

  if (isFiniteNumber(absoluteValue) && isFiniteNumber(absoluteMax) && absoluteMax > 0) {
    return clampScore((absoluteValue / absoluteMax) * 5)
  }

  if (isFiniteNumber(absoluteValue)) {
    return clampScore(absoluteValue)
  }



  return null
}

