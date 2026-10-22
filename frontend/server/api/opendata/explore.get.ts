import type { ProductDto } from '~~/shared/api-client'
import { ProductsIncludeEnum } from '~~/shared/api-client'
import { useProductService } from '~~/shared/api-client/services/products.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import {
  OPENDATA_EXPLORE_CATEGORIES,
  OPENDATA_EXPLORE_PRODUCTS_PER_CATEGORY,
  type OpenDataExploreCategoryProducts,
  type OpenDataExploreProduct,
} from '~~/shared/utils/opendata-explore'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

const CACHE_TTL_MS = 60 * 60 * 1000
const SORT_FIELD = 'scores.ECOSCORE.value'

const cache = new Map<
  string,
  { expiresAt: number; value: OpenDataExploreCategoryProducts[] }
>()

const toExploreProduct = (
  product: ProductDto
): OpenDataExploreProduct | null => {
  const gtin = product.gtin != null ? String(product.gtin).trim() : ''
  const href = product.fullSlug?.trim()

  if (!gtin || !href) {
    return null
  }

  return {
    gtin,
    href: href.startsWith('/') ? href : `/${href}`,
    name:
      product.names?.cardName?.trim() ||
      product.names?.displayName?.trim() ||
      gtin,
  }
}

/**
 * Top products (by impact score) of the open data showcase categories.
 * A category in error is returned empty so the page can hide it.
 */
export default defineEventHandler(
  async (event): Promise<OpenDataExploreCategoryProducts[]> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=3600, s-maxage=3600')

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const cached = cache.get(domainLanguage)
    if (cached && cached.expiresAt > Date.now()) {
      return cached.value
    }

    const productService = useProductService(domainLanguage)

    const results = await Promise.all(
      OPENDATA_EXPLORE_CATEGORIES.map(
        async (category): Promise<OpenDataExploreCategoryProducts> => {
          try {
            const response = await productService.searchProducts({
              verticalId: category.id,
              pageNumber: 0,
              pageSize: OPENDATA_EXPLORE_PRODUCTS_PER_CATEGORY,
              include: [ProductsIncludeEnum.Base, ProductsIncludeEnum.Names],
              body: { sort: { sorts: [{ field: SORT_FIELD, order: 'desc' }] } },
            })
            const products = (response.products?.data ?? [])
              .map(toExploreProduct)
              .filter((p): p is OpenDataExploreProduct => p !== null)
            return { id: category.id, products }
          } catch (error) {
            console.error(
              `opendata:explore failed for ${category.id}`,
              error instanceof Error ? error.message : error
            )
            return { id: category.id, products: [] }
          }
        }
      )
    )

    // Do not cache a fully failed round, retry on the next request.
    if (results.some(category => category.products.length > 0)) {
      cache.set(domainLanguage, {
        expiresAt: Date.now() + CACHE_TTL_MS,
        value: results,
      })
    }

    return results
  }
)
