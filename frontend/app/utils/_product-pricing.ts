import type { ProductDto } from '~~/shared/api-client'

type NumberFormatFn = (
  value: number,
  options?: Intl.NumberFormatOptions
) => string
type TranslateFn = (key: string, params?: Record<string, unknown>) => string
type PluralizeFn = (
  key: string,
  count: number,
  params?: Record<string, unknown>
) => string

export const formatBestPrice = (
  product: ProductDto,
  t: TranslateFn,
  n: NumberFormatFn
): string => {
  const price = product.offers?.bestPrice?.price
  const currency = product.offers?.bestPrice?.currency

  if (price == null) {
    return t('category.products.priceUnavailable')
  }

  if (currency) {
    try {
      return n(price, { style: 'currency', currency })
    } catch {
      return `${n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`.trim()
    }
  }

  return n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export const formatOffersCount = (
  product: ProductDto,
  translatePlural: PluralizeFn
): string => {
  const count = product.offers?.offersCount ?? 0
  return translatePlural('category.products.offerCount', count, { count })
}
