const GTIN_CAPTURE_PATTERN = /^(\d{6,})(?:$|[-_].*)$/
const GTIN_INLINE_PATTERN = /(?<!\d)(\d{8,14})(?!\d)/

export const isValidGtinParam = (value: unknown): value is string => {
  return typeof value === 'string' && GTIN_CAPTURE_PATTERN.test(value)
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

export const findGtinInText = (value: unknown): string | null => {
  if (typeof value !== 'string') {
    return null
  }

  const match = value.match(GTIN_INLINE_PATTERN)
  return match ? match[1] : null
}
