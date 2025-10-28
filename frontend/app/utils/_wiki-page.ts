export const normalizeWikiPageId = (
  value: string | null | undefined,
): string | null => {
  if (!value) {
    return null
  }

  const trimmed = value.trim()
  if (!trimmed) {
    return null
  }

  const sanitize = (input: string): string => {
    const withoutOrigin = input.replace(/^https?:\/\/[^/]+\//i, '')
    const withoutPagesPrefix = withoutOrigin.replace(/^pages\//i, '')

    return withoutPagesPrefix.replace(/^\/+/, '')
  }

  try {
    const decoded = decodeURIComponent(trimmed)
    return sanitize(decoded)
  } catch {
    return sanitize(trimmed)
  }
}
