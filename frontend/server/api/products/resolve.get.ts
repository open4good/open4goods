import { createError, defineEventHandler, getQuery } from 'h3'
import type { FilterRequestDto, ProductDto } from '~~/shared/api-client'
import { useProductService } from '~~/shared/api-client/services/products.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { normaliseProductDto } from '../../utils/normalise-product-sourcing'

type ResolveQuery = {
  gtin?: string | string[]
  brand?: string | string[]
  model?: string | string[]
}

type ResolvedBy = 'gtin' | 'brand-model'

interface ProductResolveResponse {
  product: ProductDto | null
  resolvedBy: ResolvedBy | null
  confidence: 'high' | 'low' | null
  reason?: string
}

const normalizeInput = (value: string) =>
  value
    .toLocaleLowerCase()
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()

const normalizeMaybeArray = (value?: string | string[]): string => {
  if (Array.isArray(value)) {
    return (value[0] ?? '').trim()
  }

  return typeof value === 'string' ? value.trim() : ''
}

const resolveComparableBrandModel = (product: ProductDto) => {
  const brand = (product.identity?.brand ?? '').trim()
  const model = (product.identity?.model ?? '').trim()

  return {
    brand,
    model,
    normalizedBrand: normalizeInput(brand),
    normalizedModel: normalizeInput(model),
  }
}

export default defineEventHandler(
  async (event): Promise<ProductResolveResponse> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=120, s-maxage=120')

    const query = getQuery<ResolveQuery>(event)
    const gtin = normalizeMaybeArray(query.gtin)
    const brand = normalizeMaybeArray(query.brand)
    const model = normalizeMaybeArray(query.model)

    if (!gtin && !(brand && model)) {
      throw createError({
        statusCode: 400,
        statusMessage:
          'Provide either gtin or both brand and model query parameters.',
      })
    }

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)
    const productService = useProductService(domainLanguage)

    if (gtin) {
      const parsed = Number.parseInt(gtin, 10)

      if (!Number.isFinite(parsed)) {
        throw createError({
          statusCode: 400,
          statusMessage: 'gtin must be numeric.',
        })
      }

      try {
        const product = await productService.getProductByGtin(parsed)

        return {
          product: normaliseProductDto(product),
          resolvedBy: 'gtin',
          confidence: 'high',
        }
      } catch (error) {
        const backendError = await extractBackendErrorDetails(error)

        if (
          backendError.isResponseError &&
          backendError.statusCode >= 400 &&
          backendError.statusCode < 500
        ) {
          return {
            product: null,
            resolvedBy: null,
            confidence: null,
            reason: 'not-found',
          }
        }

        throw createError({
          statusCode: backendError.statusCode,
          statusMessage: backendError.statusMessage,
          cause: error,
        })
      }
    }

    const normalizedBrand = normalizeInput(brand)
    const normalizedModel = normalizeInput(model)

    const filters: FilterRequestDto = {
      filters: [
        {
          field: 'attributes.referentielAttributes.BRAND',
          operator: 'term',
          terms: [brand],
        },
      ],
    }

    try {
      const result = await productService.searchProducts({
        query: `${brand} ${model}`,
        pageNumber: 0,
        pageSize: 6,
        filters,
      })

      const candidates = (result.products?.data ?? []).map(product => {
        const comparable = resolveComparableBrandModel(product)
        const exactBrand = comparable.normalizedBrand === normalizedBrand
        const exactModel = comparable.normalizedModel === normalizedModel

        return {
          product,
          exactBrand,
          exactModel,
          isExact: exactBrand && exactModel,
        }
      })

      const exactMatches = candidates.filter(item => item.isExact)

      if (exactMatches.length !== 1) {
        return {
          product: null,
          resolvedBy: null,
          confidence: null,
          reason: exactMatches.length === 0 ? 'not-found' : 'ambiguous',
        }
      }

      return {
        product: normaliseProductDto(exactMatches[0].product),
        resolvedBy: 'brand-model',
        confidence: 'high',
      }
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)

      throw createError({
        statusCode: backendError.statusCode,
        statusMessage: backendError.statusMessage,
        cause: error,
      })
    }
  }
)
