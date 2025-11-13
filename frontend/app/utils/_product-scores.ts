import type { ProductDto } from '~~/shared/api-client'

const clampScore = (score: number) => Math.max(0, Math.min(score, 5))

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

  if (impactEntry.on20 != null && Number.isFinite(impactEntry.on20)) {
    return clampScore((impactEntry.on20 / 20) * 5)
  }

  if (impactEntry.percent != null && Number.isFinite(impactEntry.percent)) {
    return clampScore((impactEntry.percent / 100) * 5)
  }

  if (impactEntry.value != null && impactEntry.absolute?.max) {
    const max = impactEntry.absolute.max

    if (Number.isFinite(max) && max > 0) {
      return clampScore((impactEntry.value / max) * 5)
    }
  }

  if (impactEntry.value != null && Number.isFinite(impactEntry.value)) {
    return clampScore(impactEntry.value)
  }

  const relativValue = impactEntry.relativ?.value
  if (relativValue != null && Number.isFinite(relativValue)) {
    return clampScore(relativValue)
  }

  return null
}

