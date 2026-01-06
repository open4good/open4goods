import type { ProductDto } from '~~/shared/api-client'

const clampScore = (score: number) => Math.max(0, Math.min(score, 5))

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
