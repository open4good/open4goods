export interface ProductRouteMatch {
  categorySlug: string | null
  gtin: string
  slug: string
}

const CATEGORY_SLUG_PATTERN = /^[a-z]+(?:-[a-z]+)*$/
const PRODUCT_SEGMENT_PATTERN = /^(?<gtin>\d{6,})-(?<slug>[a-z0-9-]+)$/i

export const matchProductRouteFromSegments = (
  segments: readonly string[],
): ProductRouteMatch | null => {
  if (segments.length === 0 || segments.length > 2) {
    return null
  }

  let rawCategorySlug: string | undefined
  let rawProductSegment: string | undefined

  if (segments.length === 1) {
    ;[rawProductSegment] = segments
  } else {
    ;[rawCategorySlug, rawProductSegment] = segments
  }

  const categorySlug = rawCategorySlug?.trim().toLowerCase() ?? null

  if (rawCategorySlug != null) {
    if (!categorySlug || !CATEGORY_SLUG_PATTERN.test(categorySlug)) {
      return null
    }
  }

  const productMatch = rawProductSegment?.trim().match(PRODUCT_SEGMENT_PATTERN)

  if (!productMatch?.groups) {
    return null
  }

  const { gtin, slug: rawSlug } = productMatch.groups as {
    gtin: string
    slug: string
  }

  if (!gtin || gtin.length < 6 || !rawSlug) {
    return null
  }

  return {
    categorySlug,
    gtin,
    slug: rawSlug.toLowerCase(),
  }
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
