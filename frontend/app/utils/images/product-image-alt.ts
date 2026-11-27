import type { ProductDto } from '~~/shared/api-client'

export interface ProductImageAltOptions {
  /** Product type label (« Lave-linge »), when the caller knows it. */
  productType?: string | null
  /** Human view label (« vue de face »), when known. Never a file name. */
  view?: string | null
  /** Used when the product carries neither brand nor model. */
  fallback?: string
}

const clean = (value: string | null | undefined): string =>
  typeof value === 'string' ? value.trim() : ''

/**
 * Descriptive alt text built from product data only:
 * « {type} {brand} {model}, {view} ». The image file name is never used.
 */
export const buildProductImageAlt = (
  product: ProductDto,
  options: ProductImageAltOptions = {}
): string => {
  const brand = clean(product.identity?.brand)
  const model = clean(product.identity?.model)
  const identity =
    brand && model
      ? [clean(options.productType), brand, model].filter(Boolean).join(' ')
      : ''
  const base =
    identity ||
    clean(product.identity?.bestName) ||
    clean(product.base?.bestName) ||
    [brand, model].filter(Boolean).join(' ') ||
    clean(options.fallback)
  const view = clean(options.view)
  return view && base ? `${base}, ${view}` : base
}
