import type { ProductAggregatedPriceDto } from '~~/shared/api-client'

type Translate = (key: string, ...args: unknown[]) => string
type NumberFormat = (
  value: number,
  options?: Intl.NumberFormatOptions
) => string

export type OfferShippingLabels = {
  cost: string | null
  delay: string | null
}

const isValidNumber = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value)

/**
 * Libellés de livraison d'une offre. Une donnée absente ou invalide ne produit
 * aucun libellé (jamais de « offerte » ni de délai par défaut).
 */
export const resolveOfferShippingLabels = (
  offer:
    | Pick<
        ProductAggregatedPriceDto,
        'shippingCost' | 'shippingTimeDays' | 'currency'
      >
    | null
    | undefined,
  t: Translate,
  n: NumberFormat
): OfferShippingLabels => {
  let cost: string | null = null
  const shippingCost = offer?.shippingCost
  if (isValidNumber(shippingCost) && shippingCost >= 0) {
    if (shippingCost === 0) {
      cost = t('product.price.shipping.free')
    } else if (offer?.currency) {
      try {
        const amount = n(shippingCost, {
          style: 'currency',
          currency: offer.currency,
        })
        cost = t('product.price.shipping.cost', { amount })
      } catch {
        cost = null
      }
    }
  }

  const days = offer?.shippingTimeDays
  const delay =
    isValidNumber(days) && days >= 0
      ? t('product.price.shipping.delay', { count: days }, days)
      : null

  return { cost, delay }
}
