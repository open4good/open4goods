import type { ProductDto } from '~~/shared/api-client'

const clampScore = (score: number) => Math.max(0, Math.min(score, 5))
const clampScoreOn20 = (score: number) => Math.max(0, Math.min(score, 20))
const isFiniteNumber = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value)

/**
 * Resolve the ECOSCORE value for a product on a five-point scale.
 * The ECOSCORE value is already expected to be between 0 and 5.
 */
export const resolvePrimaryImpactScore = (
  product: ProductDto
): number | null => {
  const value = product.scores?.scores?.ECOSCORE?.value

  if (!isFiniteNumber(value)) {
    return null
  }

  return clampScore(value)
}

/**
 * Resolve the ECOSCORE value for a product on a twenty-point scale.
 * Prefers the explicit on20 field, then derives from the 0-5 value if needed.
 */
export const resolvePrimaryImpactScoreOn20 = (
  product: ProductDto
): number | null => {
  const score = product.scores?.scores?.ECOSCORE ?? product.scores?.ecoscore

  if (!score) {
    return null
  }

  const on20 = score.on20
  if (isFiniteNumber(on20)) {
    return clampScoreOn20(on20)
  }

  const value = score.value
  if (isFiniteNumber(value)) {
    if (value <= 5) {
      return clampScoreOn20(value * 4)
    }

    if (value <= 20) {
      return clampScoreOn20(value)
    }
  }

  return null
}
