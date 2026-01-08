import type { ProductDto } from '~~/shared/api-client'
import { humanizeSlug } from '~/utils/_product-title'

export const resolveProductTitle = (
  product: ProductDto,
  locale?: string
): string => {
  const normalizeString = (value: string | null | undefined) =>
    typeof value === 'string' ? value.trim() : ''

  // 1. Pretty Name / AI Medium Title
  const prettyName = normalizeString(product.names?.prettyName)
  if (prettyName) return prettyName

  const aiTitle = normalizeString(product.aiReview?.review?.mediumTitle)
  if (aiTitle) return aiTitle

  // 2. Best Name
  const identityBestName = normalizeString(product.identity?.bestName)
  if (identityBestName) return identityBestName

  const baseBestName = normalizeString(product.base?.bestName)
  if (baseBestName) return baseBestName

  // 3. Brand - Model
  const brand = normalizeString(product.identity?.brand)
  const model = normalizeString(product.identity?.model)
  if (brand && model) return `${brand} - ${model}`
  if (model) return model // If only model is present, it's better than nothing, although priority says "Brand - Model"

  // 4. Best Offer Name
  const bestOfferName = normalizeString(product.offers?.bestPrice?.offerName)
  if (bestOfferName) return bestOfferName

  // Fallbacks (from original ProductHero logic)
  const namesH1 = normalizeString(product.names?.h1Title)
  if (namesH1) return namesH1

  const slug = normalizeString(product.slug)
  if (slug && locale) return humanizeSlug(slug, locale)

  const gtin = normalizeString(product.gtin?.toString())
  if (gtin) return `GTIN: ${gtin}` // Or localized "GTIN: ..." if we had access to i18n here, but this is a utility.

  return ''
}
