const GTIN_ONLY_PATTERN = /^\d{6,}$/
const GTIN_CAPTURE_PATTERN = /^(\d{6,})(?:$|[-_].*)$/

export const isValidGtinParam = (value: unknown): value is string => {
  return typeof value === 'string' && GTIN_ONLY_PATTERN.test(value)
}

export const extractGtinParam = (value: unknown): string | null => {
  if (typeof value === 'string') {
    const match = value.match(GTIN_CAPTURE_PATTERN)
    return match ? match[1] : null
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      if (typeof item !== 'string') {
        continue
      }

      const match = item.match(GTIN_CAPTURE_PATTERN)
      if (match) {
        return match[1]
      }
    }
  }

  return null
}

export const extractRawGtinParam = (value: unknown): string | null => {
  if (typeof value === 'string' && GTIN_ONLY_PATTERN.test(value)) {
    return value
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      if (typeof item === 'string' && GTIN_ONLY_PATTERN.test(item)) {
        return item
      }
    }
  }

  return null
}

export const isValidRawGtin = (value: unknown): value is string => {
  return typeof value === 'string' && GTIN_ONLY_PATTERN.test(value)
}
