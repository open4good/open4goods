import {
  ProductIncludeEnum,
  type ProductIncludeEnum as ProductIncludeEnumType,
} from '~~/shared/api-client/apis/ProductApi'

const VALID_PRODUCT_INCLUDES = new Set<string>(Object.values(ProductIncludeEnum))

/**
 * Parse and validate include values accepted by ProductApi.product().
 */
export const parseProductIncludes = (
  rawInclude: unknown
): ProductIncludeEnumType[] => {
  const values =
    typeof rawInclude === 'string'
      ? rawInclude.split(',')
      : Array.isArray(rawInclude)
        ? rawInclude.flatMap(value =>
            typeof value === 'string' ? value.split(',') : []
          )
        : []

  const deduped = new Set<ProductIncludeEnumType>()

  values.forEach(value => {
    const normalized = value.trim()
    if (VALID_PRODUCT_INCLUDES.has(normalized)) {
      deduped.add(normalized as ProductIncludeEnumType)
    }
  })

  return Array.from(deduped)
}

