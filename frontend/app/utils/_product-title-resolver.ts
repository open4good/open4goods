import type { ProductDto } from '~~/shared/api-client'

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

const normalizeString = (value: string | number | null | undefined): string =>
  typeof value === 'string' || typeof value === 'number'
    ? value.toString().trim()
    : ''

const containsRawTemplate = (value: string): boolean =>
  /\[\(\$\{[^}]+}\)\]|\$\{[^}]+}|\{[A-Z0-9_]+}/.test(value)

const safeTitle = (value: string | null | undefined): string => {
  const normalized = normalizeString(value)
  return normalized && !containsRawTemplate(normalized) ? normalized : ''
}

const brandModelTitle = (product: ProductDto): string => {
  const brand = normalizeString(product.identity?.brand)
  const model = normalizeString(product.identity?.model)
  return [brand, model].filter(Boolean).join(' ')
}

const fallbackTitle = (
  product: ProductDto,
  locale?: string,
  uppercaseBrand = false,
  gtinFallback?: string
): string => {
  const brand = normalizeString(product.identity?.brand)
  const candidates = [
    product.identity?.bestName,
    product.base?.bestName,
    brandModelTitle(product),
    normalizeString(product.gtin) ? (gtinFallback || `GTIN: ${product.gtin}`) : '',
  ]

  for (const candidate of candidates) {
    const title = safeTitle(candidate)
    if (title) {
      return uppercaseBrand ? applyBrandCasing(title, brand, locale) : title
    }
  }

  return ''
}

export const resolveProductTitle = (
  product: ProductDto,
  locale?: string,
  options: ProductTitleOptions = {}
): string => {
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

  const primary = preferCardTitle
    ? product.names?.cardName
    : preferLongName || preferH1Title
      ? product.names?.pageTitle
      : preferShortName || preferPrettyName
        ? product.names?.displayName
        : product.names?.displayName

  const title = safeTitle(primary)
  if (title) {
    return uppercaseBrand ? applyBrandCasing(title, brand, locale) : title
  }

  return fallbackTitle(product, locale, uppercaseBrand, gtinFallback)
}

export const resolveProductShortName = (
  product: ProductDto,
  locale?: string
): string =>
  safeTitle(product.names?.displayName) || fallbackTitle(product, locale)

export const resolveProductCardName = (
  product: ProductDto,
  locale?: string
): string =>
  safeTitle(product.names?.cardName) ||
  fallbackTitle(product, locale)

export const resolveProductLongName = (
  product: ProductDto,
  locale?: string
): string =>
  (safeTitle(product.names?.pageTitle)
    ? applyBrandCasing(
        safeTitle(product.names?.pageTitle),
        normalizeString(product.identity?.brand),
        locale
      )
    : '') ||
  fallbackTitle(product, locale, true)
