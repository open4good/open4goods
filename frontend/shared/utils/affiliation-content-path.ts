export const AFFILIATION_CONTENT_QUERY_PARAM = 'from'
export const AFFILIATION_CONTENT_MAX_LENGTH = 200

/** Same rules as `X-Affiliation-Origin`: bare path, single leading `/`, no
 * `?`, `#`, whitespace or control character, 200 characters max. */
const BARE_PATH = /^\/(?!\/)[^?#\s]*$/

const hasControlCharacter = (value: string): boolean =>
  [...value].some(char => {
    const code = char.charCodeAt(0)
    return code < 0x20 || code === 0x7f
  })

/** Content (article or guide) the visitor read before the product page. */
const CONTENT_PREFIXES = ['/blog/', '/guides/']

export const isBlogOrGuideArticlePath = (path: string): boolean =>
  /^\/(blog|guides)\/[^/?#]+/.test(path)

/**
 * Returns the path when it can be sent as `X-Affiliation-Content`
 * (GOU-1829), otherwise null: invalid values are ignored, never an error.
 */
export const sanitizeAffiliationContentPath = (
  value: unknown
): string | null => {
  if (
    typeof value !== 'string' ||
    value.length > AFFILIATION_CONTENT_MAX_LENGTH ||
    !BARE_PATH.test(value) ||
    hasControlCharacter(value)
  ) {
    return null
  }

  return CONTENT_PREFIXES.some(
    prefix => value.startsWith(prefix) && value.length > prefix.length
  )
    ? value
    : null
}
