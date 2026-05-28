export const capitalizeFirstLetter = (
  value: string,
  locale?: string
): string => {
  const trimmed = value.trim()
  if (!trimmed) {
    return ''
  }

  const [first, ...rest] = Array.from(trimmed)

  return `${first.toLocaleUpperCase(locale)}${rest.join('')}`
}

export const formatBrandModelTitle = (
  brand: string,
  model: string,
  locale?: string
): string => {
  const formattedBrand = capitalizeFirstLetter(brand, locale)
  const formattedModel = capitalizeFirstLetter(model, locale)

  return [formattedBrand, formattedModel].filter(Boolean).join(' ')
}

export const humanizeSlug = (slug: string, locale?: string): string => {
  return slug
    .split('-')
    .map(part => capitalizeFirstLetter(part, locale))
    .join(' ')
    .trim()
}

export interface ProductDisplayNameOptions {
  fallbackLabel: string
}

/**
 * Resolve a product display name for SSR-safe rendering.
 */
export const resolveProductDisplayName = (
  product: {
    identity?: {
      bestName?: string | null
      brand?: string | null
      model?: string | null
    } | null
    base?: { bestName?: string | null } | null
    names?: { displayName?: string | null; pageTitle?: string | null } | null
  },
  { fallbackLabel }: ProductDisplayNameOptions
): string => {
  const normalize = (value?: string | null) => value?.trim() ?? ''
  const brand = normalize(product.identity?.brand)
  const model = normalize(product.identity?.model)
  const brandModel = [brand, model].filter(Boolean).join(' - ')

  const primaryCandidates = [
    normalize(product.names?.displayName),
    normalize(product.identity?.bestName),
    normalize(product.base?.bestName),
    normalize(product.names?.pageTitle),
    normalize(product.identity?.model),
    normalize(product.identity?.brand),
    brandModel,
  ].filter(Boolean)

  return primaryCandidates[0] ?? fallbackLabel
}
