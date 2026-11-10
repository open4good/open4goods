import type {
  GlobalSearchResponseDto,
  GlobalSearchResultDto,
  ProductDto,
  ProductSearchResponseDto,
} from '~~/shared/api-client'

/**
 * Score entries the search result cards read (`resolvePrimaryImpactScore`,
 * `ECOSCORE_RELATIVE_FIELD`). Every other entry of `scores.scores` is dropped.
 */
const KEPT_SCORE_KEYS = ['ECOSCORE'] as const

/**
 * Reduce a product to the fields the search result cards render.
 *
 * The search page is server-rendered, so every product it receives is
 * serialised into the HTML payload. A full `ProductDto` weighs about 35 kB,
 * mostly `attributes.allAttributes`, the price histories and the image
 * gallery, none of which a result card shows. Cards read the identity, names,
 * indexed and referential attributes, the cover image (or the first image),
 * the ECOSCORE and impact-score status, and the offers summary.
 *
 * @param product full product returned by the search API
 * @returns a shallow copy without the fields the result cards never read
 */
export const slimSearchProduct = (product: ProductDto): ProductDto => {
  const {
    eprel: _eprel,
    timeline: _timeline,
    datasources: _datasources,
    ...rest
  } = product

  const slim: ProductDto = { ...rest }

  if (product.attributes) {
    const { allAttributes: _allAttributes, ...attributes } = product.attributes
    slim.attributes = attributes
  }

  if (product.offers) {
    const {
      newHistory: _newHistory,
      occasionHistory: _occasionHistory,
      ...offers
    } = product.offers
    slim.offers = offers
  }

  if (product.resources) {
    const firstImage = product.resources.images?.[0]
    slim.resources = {
      coverImagePath: product.resources.coverImagePath,
      externalCover: product.resources.externalCover,
      ...(firstImage ? { images: [firstImage] } : {}),
    }
  }

  if (product.scores) {
    const keptScores = Object.fromEntries(
      KEPT_SCORE_KEYS.flatMap(key => {
        const score = product.scores?.scores?.[key]
        return score ? [[key, score]] : []
      })
    )
    slim.scores = { ...product.scores, scores: keptScores }
  }

  return slim
}

const slimResults = (
  results: GlobalSearchResultDto[] | undefined
): GlobalSearchResultDto[] | undefined =>
  results?.map(result =>
    result.product
      ? { ...result, product: slimSearchProduct(result.product) }
      : result
  )

/**
 * Slim every product of a global search response (vertical groups and
 * products without a vertical) with {@link slimSearchProduct}.
 *
 * @param response global search response, or null when no search ran
 * @returns the same response with slimmed products
 */
export const slimGlobalSearchResponse = (
  response: GlobalSearchResponseDto | null
): GlobalSearchResponseDto | null => {
  if (!response) {
    return response
  }

  return {
    ...response,
    verticalGroups: response.verticalGroups?.map(group => ({
      ...group,
      results: slimResults(group.results),
    })),
    missingVerticalResults: slimResults(response.missingVerticalResults),
  }
}

/**
 * Slim the products of a product search response with
 * {@link slimSearchProduct}, keeping aggregations and paging untouched.
 *
 * @param response product search response, or null when no search ran
 * @returns the same response with slimmed products
 */
export const slimProductSearchResponse = (
  response: ProductSearchResponseDto | null
): ProductSearchResponseDto | null => {
  if (!response?.products?.data) {
    return response
  }

  return {
    ...response,
    products: {
      ...response.products,
      data: response.products.data.map(slimSearchProduct),
    },
  }
}
