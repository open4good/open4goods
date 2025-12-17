export interface SlugifyOptions {
  fallback?: string
}

/**
 * Convert a human readable string into a URL/XWiki friendly slug.
 * Accents are stripped and non-alphanumeric characters collapse into hyphens.
 */
export const _slugify = (
  input: string,
  options: SlugifyOptions = {}
): string => {
  if (!input) {
    return options.fallback ?? ''
  }

  const normalized = input
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/['’]/g, '')

  const slug = normalized
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase()

  if (!slug) {
    return options.fallback ?? ''
  }

  return slug
}
