import type { ProductDto } from '~~/shared/api-client'

const clampScore = (score: number) => Math.max(0, Math.min(score, 5))
const isFiniteNumber = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value)
const toNumber = (value: unknown): number | null => {
  if (isFiniteNumber(value)) {
    return value
  }

  if (typeof value === 'string') {
    const parsed = Number(value)

    return Number.isFinite(parsed) ? parsed : null
  }

  return null
}

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

  const on20Value = toNumber(impactEntry.on20)

  if (on20Value !== null) {
    return clampScore((on20Value / 20) * 5)
  }

  const percentValue = toNumber(impactEntry.percent)

  if (percentValue !== null) {
    return clampScore((percentValue / 100) * 5)
  }

  const absoluteValue = toNumber(impactEntry.absolute?.value)
  const absoluteMax = toNumber(impactEntry.absolute?.max)

  if (absoluteValue !== null && absoluteMax !== null && absoluteMax > 0) {
    return clampScore((absoluteValue / absoluteMax) * 5)
  }

  if (absoluteValue !== null) {
    return clampScore(absoluteValue)
  }

  const rawValue = toNumber(impactEntry.value)

  if (rawValue !== null) {
    return clampScore(rawValue)
  }

  const relativeValue = toNumber(impactEntry.relativeValue)

  if (relativeValue !== null) {
    return clampScore(relativeValue)
  }

  const relativValue = toNumber(impactEntry.relativ?.value)

  if (relativValue !== null) {
    return clampScore(relativValue)
  }



  return null
}

