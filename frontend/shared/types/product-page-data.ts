import type {
  AggregationResponseDto,
  CommercialEvent,
  ProductDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'

/**
 * Product page SSR payload used to reduce request fan-out.
 */
export interface ProductPageData {
  product: ProductDto | null
  categoryDetail: VerticalConfigFullDto | null
  aggregations: Record<string, AggregationResponseDto>
  commercialEvents: CommercialEvent[]
}
