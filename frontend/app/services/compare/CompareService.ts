import type { ProductDto, VerticalConfigFullDto } from '~~/shared/api-client'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { resolveProductTitle } from '~/utils/_product-title-resolver'

export interface CompareProductEntry {
  gtin: string
  product: ProductDto
  verticalId: string | null
  title: string
  brand: string | null
  model: string | null
  coverImage: string | null
  impactScore: number | null
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

const resolveTitle = (product: ProductDto): string => {
  return (
    resolveProductTitle(product, undefined, {
      preferLongName: true,
      preferH1Title: true,
    }) || '#'
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
        try {
          const product = await fetchProduct(gtin)
          const verticalId = product.base?.vertical ?? null

          return {
            gtin,
            product,
            verticalId,
            title: resolveTitle(product),
            brand: product.identity?.brand ?? null,
            model: product.identity?.model ?? null,
            coverImage: resolveCoverImage(product),
            impactScore: resolvePrimaryImpactScore(product),
            country: resolveCountry(product),
          }
        } catch (error) {
          console.warn(`Failed to load product ${gtin} for comparison`, error)
          return null
        }
      })
    )

    return products.filter((p): p is CompareProductEntry => p !== null)
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
