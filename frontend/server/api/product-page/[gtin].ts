import {
  createError,
  defineEventHandler,
  getQuery,
  getRouterParam,
} from 'h3'
import { useCategoriesService } from '~~/shared/api-client/services/categories.services'
import { useCommercialEventsService } from '~~/shared/api-client/services/commercial-events.services'
import { useProductService } from '~~/shared/api-client/services/products.services'
import {
  AggTypeEnum,
  type Agg,
  type AggregationResponseDto,
  type ProductSearchResponseDto,
} from '~~/shared/api-client'
import type { ProductPageData } from '~~/shared/types/product-page-data'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { normaliseProductDto } from '../../utils/normalise-product-sourcing'
import { parseProductIncludes } from '../../utils/product-include'

const resolveScoreIds = (categoryDetail: ProductPageData['categoryDetail']) => {
  const ids: string[] = ['ECOSCORE']
  const ponderations = categoryDetail?.impactScoreConfig?.criteriasPonderation ?? {}

  Object.keys(ponderations).forEach(key => {
    const normalized = key.trim()
    if (normalized.length > 0 && !ids.includes(normalized)) {
      ids.push(normalized)
    }
  })

  return ids
}

const resolveCategoryBySlug = async (
  slug: string,
  domainLanguage: 'en' | 'fr'
): Promise<ProductPageData['categoryDetail']> => {
  const categoriesService = useCategoriesService(domainLanguage)
  const categories = await categoriesService.getCategories()
  const category = categories.find(
    item => (item.verticalHomeUrl?.replace(/^\//, '') ?? '') === slug
  )

  if (!category?.id) {
    return null
  }

  return categoriesService.getCategoryById(category.id)
}

export default defineEventHandler(async (event): Promise<ProductPageData> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=300, s-maxage=300')

  const gtinParam = getRouterParam(event, 'gtin')
  if (!gtinParam) {
    throw createError({ statusCode: 400, statusMessage: 'Product GTIN is required' })
  }

  const parsedGtin = Number.parseInt(gtinParam, 10)
  if (!Number.isFinite(parsedGtin)) {
    throw createError({ statusCode: 400, statusMessage: 'Product GTIN must be numeric' })
  }

  const query = getQuery(event)
  const include = parseProductIncludes(query.include)
  const categorySlug = typeof query.categorySlug === 'string' ? query.categorySlug.trim() : ''

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const productService = useProductService(domainLanguage)
  const commercialEventsService = useCommercialEventsService(domainLanguage)

  try {
    const product = normaliseProductDto(
      await productService.getProductByGtin(parsedGtin, include)
    )

    let categoryDetail: ProductPageData['categoryDetail'] = null
    if (categorySlug.length > 0) {
      try {
        categoryDetail = await resolveCategoryBySlug(categorySlug, domainLanguage)
      } catch (categoryError) {
        console.error('Failed to resolve category detail for product page.', categoryError)
      }
    }

    const scoreIds = resolveScoreIds(categoryDetail)
    let aggregations: Record<string, AggregationResponseDto> = {}

    if (categoryDetail?.id && scoreIds.length > 0) {
      const aggs: Agg[] = scoreIds.map(scoreId => ({
        name: `score_${scoreId}`,
        field: `scores.${scoreId}.value`,
        type: AggTypeEnum.Range,
        step: 0.5,
      }))

      try {
        const response = await productService.searchProducts({
          verticalId: categoryDetail.id,
          pageSize: 0,
          body: { aggs: { aggs } },
        })

        aggregations = (response as ProductSearchResponseDto).aggregations?.reduce<
          Record<string, AggregationResponseDto>
        >((accumulator, aggregation) => {
          if (aggregation.name) {
            accumulator[aggregation.name] = aggregation
          }
          return accumulator
        }, {}) ?? {}
      } catch (aggregationError) {
        console.error('Failed to fetch impact aggregations', aggregationError)
      }
    }

    let commercialEvents: ProductPageData['commercialEvents'] = []
    try {
      commercialEvents = await commercialEventsService.listCommercialEvents()
    } catch (eventsError) {
      console.error('Failed to load commercial events', eventsError)
    }

    return {
      product,
      categoryDetail,
      aggregations,
      commercialEvents,
    }
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error fetching product page data:',
      backendError.logMessage,
      backendError
    )

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})
