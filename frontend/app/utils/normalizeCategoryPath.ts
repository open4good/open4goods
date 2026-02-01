export const normalizeCategoryPath = (
  path?: string | null
): string | null => {
  if (!path) {
    return null
  }

  const trimmed = path.trim()

  if (!trimmed) {
    return null
  }

  let normalized = trimmed.replace(/^\/+/u, '')

  if (normalized.startsWith('categories/')) {
    normalized = normalized.slice('categories/'.length)
  }

  normalized = normalized.replace(/^\/+/u, '').replace(/\/+$/u, '')

  return normalized || null
}
