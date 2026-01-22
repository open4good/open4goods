import type { ProductDto } from '~~/shared/api-client'
import { humanizeSlug } from '~/utils/_product-title'

export interface ProductTitleOptions {
  preferH1Title?: boolean
  preferPrettyName?: boolean
  preferCardTitle?: boolean
  preferShortName?: boolean
  preferLongName?: boolean
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
    preferCardTitle = false,
    preferShortName = false,
    preferLongName = false,
    uppercaseBrand = false,
    gtinFallback,
  } = options

  const brand = normalizeString(product.identity?.brand)
  const resolvedBrand = uppercaseBrand ? brand.toLocaleUpperCase(locale) : brand

  const finalizeTitle = (title: string): string =>
    uppercaseBrand ? applyBrandCasing(title, brand, locale) : title

  const aiShortTitle = normalizeString(product.aiReview?.review?.shortTitle)
  const aiMediumTitle = normalizeString(product.aiReview?.review?.mediumTitle)

  // 1. AI Titles (highest priority)
  if (preferLongName || preferH1Title) {
    if (aiMediumTitle) return finalizeTitle(aiMediumTitle)
  }

  if (preferCardTitle || preferShortName || preferPrettyName) {
    if (aiShortTitle) return finalizeTitle(aiShortTitle)
  }

  // 2. New Fields Priorities
  if (preferCardTitle) {
    const cardTitle = normalizeString(product.names?.cardTitle)
    if (cardTitle) return finalizeTitle(cardTitle)
  }

  if (preferShortName) {
    const shortName = normalizeString(product.names?.shortName)
    if (shortName) return finalizeTitle(shortName)
  }

  if (preferLongName) {
    const longName = normalizeString(product.names?.longName)
    if (longName) return finalizeTitle(longName)
  }

  // 2. H1 Title / Long Name equivalent
  if (preferH1Title) {
    // Prefer longName if available as it captures the "full" title intent
    const longName = normalizeString(product.names?.longName)
    if (longName) return finalizeTitle(longName)

    const h1Title = normalizeString(product.names?.h1Title)
    if (h1Title) return finalizeTitle(h1Title)
  }

  // 3. Pretty Name / Short Name equivalent (Default fallback for many cases)
  if (preferPrettyName) {
    // Prefer shortName if available
    const shortName = normalizeString(product.names?.shortName)
    if (shortName) return finalizeTitle(shortName)

    const prettyName = normalizeString(product.names?.prettyName)
    if (prettyName) return finalizeTitle(prettyName)
  }

  // 4. AI Title (fallback)
  if (aiMediumTitle) return finalizeTitle(aiMediumTitle)

  // 5. Best Name (Identity -> Base)
  const identityBestName = normalizeString(product.identity?.bestName)
  if (identityBestName) return finalizeTitle(identityBestName)

  const baseBestName = normalizeString(product.base?.bestName)
  if (baseBestName) return finalizeTitle(baseBestName)

  // 6. Brand - Model (Client-side fallback, kept for products without generated titles)
  const model = normalizeString(product.identity?.model)
  if (resolvedBrand && model) {
    return finalizeTitle(`${resolvedBrand} - ${model}`)
  }
  if (model) return finalizeTitle(model)

  // 7. Best Offer Name
  const bestOfferName = normalizeString(product.offers?.bestPrice?.offerName)
  if (bestOfferName) return finalizeTitle(bestOfferName)

  // 8. Last Resorts
  const slug = normalizeString(product.slug)
  if (slug && locale) return finalizeTitle(humanizeSlug(slug, locale))

  const gtin = normalizeString(product.gtin?.toString())
  if (gtin) {
    const fallbackValue = gtinFallback?.trim().length
      ? gtinFallback
      : `GTIN: ${gtin}`

    return finalizeTitle(fallbackValue)
  }

  return ''
}

export const resolveProductShortName = (
  product: ProductDto,
  locale?: string
): string =>
  resolveProductTitle(product, locale, {
    preferShortName: true,
    preferPrettyName: true,
  })

export const resolveProductLongName = (
  product: ProductDto,
  locale?: string
): string =>
  resolveProductTitle(product, locale, {
    preferLongName: true,
    preferH1Title: true,
    uppercaseBrand: true,
  })
