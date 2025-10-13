export const GTIN_PARAM_PATTERN = /^\d{6,}$/

export const isValidGtinParam = (value: unknown): value is string => {
  return typeof value === 'string' && GTIN_PARAM_PATTERN.test(value)
}

export const extractGtinParam = (value: unknown): string | null => {
  if (typeof value === 'string' && GTIN_PARAM_PATTERN.test(value)) {
    return value
  }

  if (Array.isArray(value)) {
    const match = value.find(
      (item): item is string => typeof item === 'string' && GTIN_PARAM_PATTERN.test(item)
    )
    return match ?? null
  }

  return null
}
