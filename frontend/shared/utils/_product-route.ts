export interface ProductRouteMatch {
  categorySlug: string | null
  gtin: string
  slug: string
}

const CATEGORY_SLUG_PATTERN = /^[a-z]+(?:-[a-z]+)*$/
const PRODUCT_SEGMENT_PATTERN = /^(?<gtin>\d{6,})-(?<slug>[\p{L}0-9-]+)$/u
const GTIN_ONLY_PATTERN = /^\d{6,}$/
const SLUG_ONLY_PATTERN = /^[\p{L}0-9]+(?:-[\p{L}0-9]+)*$/u

const normaliseCategorySlug = (value: string | undefined): string | null => {
  if (value == null) {
    return null
  }

  const slug = value.trim().toLowerCase()

  if (!slug || !CATEGORY_SLUG_PATTERN.test(slug)) {
    return null
  }

  return slug
}

const matchProductSegment = (
  productSegment: string | undefined
): { gtin: string; slug: string } | null => {
  const match = productSegment?.trim().match(PRODUCT_SEGMENT_PATTERN)

  if (!match?.groups) {
    return null
  }

  const { gtin, slug } = match.groups as { gtin: string; slug: string }

  if (!gtin || gtin.length < 6 || !slug) {
    return null
  }

  return { gtin, slug: slug.toLowerCase() }
}

const matchSplitProductSegments = (
  gtinSegment: string | undefined,
  slugSegment: string | undefined
): { gtin: string; slug: string } | null => {
  const gtin = gtinSegment?.trim() ?? ''
  const slug = slugSegment?.trim() ?? ''

  if (!GTIN_ONLY_PATTERN.test(gtin) || !SLUG_ONLY_PATTERN.test(slug)) {
    return null
  }

  return {
    gtin,
    slug: slug.toLowerCase(),
  }
}

export const matchProductRouteFromSegments = (
  segments: readonly string[]
): ProductRouteMatch | null => {
  if (segments.length === 0 || segments.length > 3) {
    return null
  }

  if (segments.length === 1) {
    const product = matchProductSegment(segments[0])

    return product ? { categorySlug: null, ...product } : null
  }

  if (segments.length === 2) {
    const [first, second] = segments

    const categorySlug = normaliseCategorySlug(first)

    if (categorySlug) {
      const product = matchProductSegment(second)

      return product ? { categorySlug, ...product } : null
    }

    const product = matchSplitProductSegments(first, second)

    return product ? { categorySlug: null, ...product } : null
  }

  const [maybeCategory, maybeGtin, maybeSlug] = segments
  const categorySlug = normaliseCategorySlug(maybeCategory)

  if (!categorySlug) {
    return null
  }

  const product = matchSplitProductSegments(maybeGtin, maybeSlug)

  return product ? { categorySlug, ...product } : null
}

export const isBackendNotFoundError = (error: unknown): boolean => {
  if (!error || typeof error !== 'object') {
    return false
  }

  const maybeError = error as {
    statusCode?: number
    response?: { status?: number }
  }

  const status = maybeError.statusCode ?? maybeError.response?.status

  return status === 404
}
