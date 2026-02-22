import type { ProductDto } from '~~/shared/api-client'

const isFiniteNumber = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value)

/**
 * Resolve the ECOSCORE value for a product on a twenty-point scale.
 * The ECOSCORE value from the backend is already between 0 and 20.
 */
export const resolvePrimaryImpactScore = (
  product: ProductDto
): number | null => {
  const value = product.scores?.scores?.ECOSCORE?.value

  if (!isFiniteNumber(value)) {
    return null
  }

  return Math.max(0, Math.min(value, 20))
}
