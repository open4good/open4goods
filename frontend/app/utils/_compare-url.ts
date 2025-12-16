import { MAX_COMPARE_ITEMS } from '~/stores/useProductCompareStore'

const HASH_SEPARATOR = 'Vs'

const sanitizeGtins = (
  values: Array<string | number | null | undefined>
): string[] => {
  const normalized = values
    .map(value => {
      if (typeof value === 'number' && Number.isFinite(value)) {
        return Math.trunc(value).toString()
      }

      if (typeof value === 'string') {
        return value.trim()
      }

      return ''
    })
    .map(value => value.replace(/[^0-9]/g, ''))
    .filter(value => value.length > 0)

  const seen = new Set<string>()

  return normalized.filter(value => {
    if (seen.has(value)) {
      return false
    }

    seen.add(value)
    return true
  })
}

export const parseCompareHash = (hash: string | null | undefined): string[] => {
  if (!hash) {
    return []
  }

  const fragment = hash.startsWith('#') ? hash.slice(1) : hash
  if (!fragment) {
    return []
  }

  const parts = fragment.split(/vs/i)
  const gtins = sanitizeGtins(parts)

  if (!gtins.length) {
    return []
  }

  return gtins.slice(0, MAX_COMPARE_ITEMS)
}

export const buildCompareHashFragment = (
  gtins: Array<string | number | null | undefined>
): string => {
  const sanitized = sanitizeGtins(gtins).slice(0, MAX_COMPARE_ITEMS)

  if (!sanitized.length) {
    return ''
  }

  return sanitized.join(HASH_SEPARATOR)
}

export const buildCompareHash = (
  gtins: Array<string | number | null | undefined>
): string => {
  const fragment = buildCompareHashFragment(gtins)
  return fragment ? `#${fragment}` : ''
}
