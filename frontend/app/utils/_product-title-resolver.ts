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

const containsIdentityToken = (title: string, identityTokens: string[]) => {
  const normalizedTitle = title.toLocaleLowerCase()

  return identityTokens.some(token =>
    normalizedTitle.includes(token.toLocaleLowerCase())
  )
}

const isWeakProductName = (title: string, identityTokens: string[]) => {
  if (!identityTokens.length || containsIdentityToken(title, identityTokens)) {
    return false
  }

  return title.split(/\s+/).filter(Boolean).length <= 3
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
  const model = normalizeString(product.identity?.model)
  const identityBestName = normalizeString(product.identity?.bestName)
  const baseBestName = normalizeString(product.base?.bestName)
  const resolvedBrand = uppercaseBrand ? brand.toLocaleUpperCase(locale) : brand
  const identityTokens = [brand, model].filter(token => token.length > 1)

  const finalizeTitle = (title: string): string =>
    uppercaseBrand ? applyBrandCasing(title, brand, locale) : title

  const useTitle = (title: string, allowWeakName = true): string => {
    if (!title) {
      return ''
    }

    if (!allowWeakName && isWeakProductName(title, identityTokens)) {
      return ''
    }

    return finalizeTitle(title)
  }

  const firstTitle = (
    candidates: Array<string | null | undefined>,
    allowWeakName = true
  ): string => {
    for (const candidate of candidates) {
      const title = useTitle(normalizeString(candidate), allowWeakName)
      if (title) {
        return title
      }
    }

    return ''
  }

  const aiShortTitle = normalizeString(product.aiReview?.review?.shortTitle)
  const aiMediumTitle = normalizeString(product.aiReview?.review?.mediumTitle)

  // Product-owned generated names are more stable than AI review titles, which
  // can collapse to the category label for some verticals.
  if (preferCardTitle) {
    const title = firstTitle(
      [
        product.names?.cardTitle,
        product.names?.shortName,
        product.names?.prettyName,
      ],
      false
    )
    if (title) return title
  }

  if (preferShortName) {
    const title = firstTitle(
      [
        product.names?.cardTitle,
        product.names?.shortName,
        product.names?.prettyName,
      ],
      false
    )
    if (title) return title
  }

  if (preferLongName || preferH1Title) {
    const title = firstTitle(
      preferH1Title
        ? [product.names?.h1Title, product.names?.longName]
        : [product.names?.longName, product.names?.h1Title],
      false
    )
    if (title) return title
  }

  if (preferPrettyName) {
    const title = firstTitle(
      [
        product.names?.cardTitle,
        product.names?.shortName,
        product.names?.prettyName,
      ],
      false
    )
    if (title) return title
  }

  // Use longest offer name if no category is associated.
  const isCategoryAssociated = Boolean(product.base?.vertical)
  if (!isCategoryAssociated && (preferCardTitle || preferShortName)) {
    const longestOfferName = normalizeString(product.names?.longestOfferName)
    if (longestOfferName) return useTitle(longestOfferName)

    const offerNames = product.names?.offerNames
    if (offerNames && offerNames.size > 0) {
      let longest = ''
      for (const name of offerNames) {
        if (name.length > longest.length) {
          longest = name
        }
      }
      if (longest) return useTitle(longest)
    }
  }

  // Identity names beat AI review titles because they are derived from product
  // identifiers rather than prose generation.
  if (identityBestName) return useTitle(identityBestName)
  if (baseBestName) return useTitle(baseBestName)

  if (resolvedBrand && model) {
    return useTitle(`${resolvedBrand} - ${model}`)
  }
  if (model) return useTitle(model)

  const aiTitle = preferLongName || preferH1Title ? aiMediumTitle : aiShortTitle
  if (aiTitle) return useTitle(aiTitle)
  if (aiMediumTitle) return useTitle(aiMediumTitle)
  if (aiShortTitle) return useTitle(aiShortTitle)

  const bestOfferName = normalizeString(product.offers?.bestPrice?.offerName)
  if (bestOfferName) return useTitle(bestOfferName)

  const slug = normalizeString(product.slug)
  if (slug && locale) return useTitle(humanizeSlug(slug, locale))

  const gtin = normalizeString(product.gtin?.toString())
  if (gtin) {
    const fallbackValue = gtinFallback?.trim().length
      ? gtinFallback
      : `GTIN: ${gtin}`

    return useTitle(fallbackValue)
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

export const resolveProductCardName = (
  product: ProductDto,
  locale?: string
): string =>
  resolveProductTitle(product, locale, {
    preferCardTitle: true,
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
