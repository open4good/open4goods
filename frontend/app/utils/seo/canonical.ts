/**
 * Build a canonical URL: origin + pathname, without query string, hash or
 * trailing slash (the root stays `/`). Returns `null` for an invalid input.
 */
export const buildCanonicalUrl = (
  path: string | null | undefined,
  origin: string
): string | null => {
  const [withoutHash = ''] = (path ?? '').split('#')
  const [pathname = ''] = withoutHash.split('?')
  const normalized =
    `/${pathname.replace(/^\/+/, '')}`.replace(/\/+$/u, '') || '/'

  try {
    return new URL(normalized, origin).toString()
  } catch {
    return null
  }
}
