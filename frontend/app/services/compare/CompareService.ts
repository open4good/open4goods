import DOMPurify from 'isomorphic-dompurify'
import type {
  ProductDto,
  VerticalConfigFullDto,
  AiReviewDto,
} from '~~/shared/api-client'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'

export interface CompareProductReview {
  description: string | null
  pros: string[]
  cons: string[]
}

export interface CompareProductEntry {
  gtin: string
  product: ProductDto
  verticalId: string | null
  title: string
  brand: string | null
  model: string | null
  coverImage: string | null
  impactScore: number | null
  review: CompareProductReview
  country: { name: string; flag?: string } | null
}

export type FetchProductFn = (gtin: string) => Promise<ProductDto>
export type FetchVerticalFn = (
  verticalId: string
) => Promise<VerticalConfigFullDto>

export interface CompareServiceOptions {
  fetchProduct?: FetchProductFn
  fetchVertical?: FetchVerticalFn
}

const stripHtml = (content: string | null): string | null => {
  if (!content) {
    return null
  }

  const sanitized = DOMPurify.sanitize(content, {
    ALLOWED_TAGS: [],
    ALLOWED_ATTR: [],
  }).trim()
  const stripped = sanitized.replace(/<[^>]+>/g, '').trim()

  return stripped.length ? stripped : null
}

const sanitizeHtml = (content: string | null): string | null => {
  if (!content) {
    return null
  }

  return DOMPurify.sanitize(content, { ADD_ATTR: ['target', 'rel', 'class'] })
}

const normaliseReview = (
  review: AiReviewDto | null | undefined
): CompareProductReview => {
  if (!review) {
    return {
      description: null,
      pros: [],
      cons: [],
    }
  }

  const description = stripHtml(review.description ?? null)
  const pros = Array.isArray(review.pros)
    ? review.pros
        .map(entry => sanitizeHtml(String(entry)))
        .filter(
          (entry): entry is string =>
            typeof entry === 'string' && entry.length > 0
        )
    : []
  const cons = Array.isArray(review.cons)
    ? review.cons
        .map(entry => sanitizeHtml(String(entry)))
        .filter(
          (entry): entry is string =>
            typeof entry === 'string' && entry.length > 0
        )
    : []

  return { description, pros, cons }
}

const resolveTitle = (product: ProductDto): string => {
  return (
    product.identity?.bestName ??
    product.base?.bestName ??
    product.names?.h1Title ??
    product.names?.longestOfferName ??
    product.identity?.model ??
    product.identity?.brand ??
    '#'
  )
}

const resolveCoverImage = (product: ProductDto): string | null => {
  return (
    product.resources?.coverImagePath ??
    product.resources?.externalCover ??
    product.resources?.images?.[0]?.url ??
    product.base?.coverImagePath ??
    null
  )
}

const resolveCountry = (
  product: ProductDto
): { name: string; flag?: string } | null => {
  const info = product.base?.gtinInfo
  if (!info?.countryName) {
    return null
  }

  return {
    name: info.countryName,
    flag: info.countryFlagUrl ?? undefined,
  }
}

const defaultFetchProduct: FetchProductFn = async gtin => {
  return await $fetch<ProductDto>(`/api/products/${gtin}`)
}

const defaultFetchVertical: FetchVerticalFn = async verticalId => {
  return await $fetch<VerticalConfigFullDto>(
    `/api/categories/${encodeURIComponent(verticalId)}`
  )
}

export const createCompareService = (options: CompareServiceOptions = {}) => {
  const fetchProduct = options.fetchProduct ?? defaultFetchProduct
  const fetchVertical = options.fetchVertical ?? defaultFetchVertical

  const loadProducts = async (
    gtins: string[]
  ): Promise<CompareProductEntry[]> => {
    const uniqueGtins = Array.from(
      new Set(gtins.filter(gtin => gtin.length > 0))
    )
    if (!uniqueGtins.length) {
      return []
    }

    const products = await Promise.all(
      uniqueGtins.map(async gtin => {
        const product = await fetchProduct(gtin)
        const verticalId = product.base?.vertical ?? null
        const review = normaliseReview(product.aiReview?.review)

        return {
          gtin,
          product,
          verticalId,
          title: resolveTitle(product),
          brand: product.identity?.brand ?? null,
          model: product.identity?.model ?? null,
          coverImage: resolveCoverImage(product),
          impactScore: resolvePrimaryImpactScore(product),
          review,
          country: resolveCountry(product),
        }
      })
    )

    return products
  }

  const loadVertical = async (
    verticalId: string | null
  ): Promise<VerticalConfigFullDto | null> => {
    if (!verticalId) {
      return null
    }

    return await fetchVertical(verticalId)
  }

  const hasMixedVerticals = (entries: CompareProductEntry[]): boolean => {
    const ids = entries
      .map(entry => entry.verticalId)
      .filter(
        (value): value is string =>
          typeof value === 'string' && value.length > 0
      )

    if (!ids.length) {
      return false
    }

    return new Set(ids).size > 1
  }

  return {
    loadProducts,
    loadVertical,
    hasMixedVerticals,
  }
}

export type CompareService = ReturnType<typeof createCompareService>
