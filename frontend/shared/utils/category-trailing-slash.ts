const CATEGORY_TRAILING_SLASH_PATTERN = /^\/([a-z]+(?:-[a-z]+)*)\/+$/

// /contrib/ answers on its own (plain text, no redirect to a canonical page).
const RESERVED_SEGMENTS = new Set(['contrib'])

/**
 * Returns the canonical path (no trailing slash) of a category URL written
 * with one (`/televiseurs/` -> `/televiseurs`), or null for any other path.
 * Only the single-segment category shape is matched: the home page, the
 * static pages, `/contrib/…` and product URLs are never rewritten.
 */
export const resolveCategoryTrailingSlashTarget = (
  pathname: string
): string | null => {
  const match = CATEGORY_TRAILING_SLASH_PATTERN.exec(pathname)

  if (!match || RESERVED_SEGMENTS.has(match[1])) {
    return null
  }

  return `/${match[1]}`
}
