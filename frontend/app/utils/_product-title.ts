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
  preferPrettyName?: boolean
}

/**
 * Resolve a product display name for SSR-safe rendering.
 * The resolution order can prefer the "prettyName" when requested and
 * falls back to a "Brand - Model" string if no explicit name is available.
 */
export const resolveProductDisplayName = (
  product: {
    identity?: {
      bestName?: string | null
      brand?: string | null
      model?: string | null
    } | null
    base?: { bestName?: string | null } | null
    names?: { prettyName?: string | null; h1Title?: string | null } | null
  },
  { fallbackLabel, preferPrettyName = false }: ProductDisplayNameOptions
): string => {
  const normalize = (value?: string | null) => value?.trim() ?? ''
  const brand = normalize(product.identity?.brand)
  const model = normalize(product.identity?.model)
  const brandModel = [brand, model].filter(Boolean).join(' - ')

  const primaryCandidates = [
    ...(preferPrettyName ? [normalize(product.names?.prettyName)] : []),
    normalize(product.identity?.bestName),
    normalize(product.base?.bestName),
    normalize(product.names?.h1Title),
    normalize(product.identity?.model),
    normalize(product.identity?.brand),
    brandModel,
  ].filter(Boolean)

  return primaryCandidates[0] ?? fallbackLabel
}
