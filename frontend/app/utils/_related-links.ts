import type { ProductDto } from '~~/shared/api-client'

export const RELATED_PRODUCTS_MAX = 12

/**
 * Keeps the neighbours worth linking: another product than the current one, with a
 * routable URL, no duplicates, capped to {@link RELATED_PRODUCTS_MAX}.
 */
export const pickRelatedProducts = (
  products: readonly ProductDto[] | null | undefined,
  currentGtin: string | number | null | undefined,
  max = RELATED_PRODUCTS_MAX
): ProductDto[] => {
  const seen = new Set<string>()
  const current = currentGtin == null ? '' : String(currentGtin)

  return (products ?? [])
    .filter(candidate => {
      const link = candidate.fullSlug?.trim()
      const gtin = candidate.gtin == null ? '' : String(candidate.gtin)
      if (!link || (gtin && gtin === current) || seen.has(link)) {
        return false
      }
      seen.add(link)
      return true
    })
    .slice(0, max)
}
