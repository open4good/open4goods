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
