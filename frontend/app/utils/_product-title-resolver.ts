import type { ProductDto } from '~~/shared/api-client'
import { humanizeSlug } from '~/utils/_product-title'

export interface ProductTitleOptions {
  preferH1Title?: boolean
  preferPrettyName?: boolean
  uppercaseBrand?: boolean
  gtinFallback?: string
}

const escapeRegExp = (value: string): string =>
  value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const applyBrandCasing = (
  title: string,
  brand: string,
  locale?: string
): string => {
  if (!brand.length) {
    return title
  }

  const normalizedBrand = brand.toLocaleUpperCase(locale)
  const regex = new RegExp(escapeRegExp(brand), 'gi')

  return title.replace(regex, normalizedBrand)
}

export const resolveProductTitle = (
  product: ProductDto,
  locale?: string,
  options: ProductTitleOptions = {}
): string => {
  const normalizeString = (value: string | null | undefined) =>
    typeof value === 'string' ? value.trim() : ''

  const {
    preferH1Title = false,
    preferPrettyName = true,
    uppercaseBrand = false,
    gtinFallback,
  } = options

  const brand = normalizeString(product.identity?.brand)
  const resolvedBrand = uppercaseBrand
    ? brand.toLocaleUpperCase(locale)
    : brand

  const finalizeTitle = (title: string): string =>
    uppercaseBrand ? applyBrandCasing(title, brand, locale) : title

  // 1. H1 Title (long form)
  if (preferH1Title) {
    const h1Title = normalizeString(product.names?.h1Title)
    if (h1Title) return finalizeTitle(h1Title)
  }

  // 2. Pretty Name / AI Medium Title
  if (preferPrettyName) {
    const prettyName = normalizeString(product.names?.prettyName)
    if (prettyName) return finalizeTitle(prettyName)
  }

  const aiTitle = normalizeString(product.aiReview?.review?.mediumTitle)
  if (aiTitle) return finalizeTitle(aiTitle)

  // 3. Best Name
  const identityBestName = normalizeString(product.identity?.bestName)
  if (identityBestName) return finalizeTitle(identityBestName)

  const baseBestName = normalizeString(product.base?.bestName)
  if (baseBestName) return finalizeTitle(baseBestName)

  // 4. Brand - Model
  const model = normalizeString(product.identity?.model)
  if (resolvedBrand && model) {
    return finalizeTitle(`${resolvedBrand} - ${model}`)
  }
  if (model) return finalizeTitle(model) // If only model is present, it's better than nothing, although priority says "Brand - Model"

  // 5. Best Offer Name
  const bestOfferName = normalizeString(product.offers?.bestPrice?.offerName)
  if (bestOfferName) return finalizeTitle(bestOfferName)

  // Fallbacks (from original ProductHero logic)
  const namesH1 = normalizeString(product.names?.h1Title)
  if (namesH1) return finalizeTitle(namesH1)

  const slug = normalizeString(product.slug)
  if (slug && locale) return finalizeTitle(humanizeSlug(slug, locale))

  const gtin = normalizeString(product.gtin?.toString())
  if (gtin) {
    const fallbackValue =
      gtinFallback?.trim().length ? gtinFallback : `GTIN: ${gtin}`

    return finalizeTitle(fallbackValue)
  }

  return ''
}
