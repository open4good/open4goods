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

export const formatPrice = (
  price: number,
  n: NumberFormatFn,
  currency?: string
): string => {
  if (currency) {
    try {
      return n(price, { style: 'currency', currency })
    } catch {
      return `${n(price, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      })} ${currency}`.trim()
    }
  }

  return n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

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

  return formatPrice(price, n, currency)
}

export const formatOffersCount = (
  product: ProductDto,
  translatePlural: PluralizeFn
): string => {
  const count = product.offers?.offersCount ?? 0
  return translatePlural('category.products.offerCount', count, { count })
}

/** rel for outbound affiliate/offer links — Google requires `sponsored` for paid links.
 * No `noreferrer`: /contrib/ needs the nudger.fr Referer (GOU-1184), see
 * AFFILIATE_LINK_REFERRER_POLICY. */
export const AFFILIATE_LINK_REL = 'sponsored nofollow noopener'

/** referrerpolicy for the same links: the Referer is sent to /contrib/ on
 * nudger.fr (navigation signal) but never to a merchant or network origin. */
export const AFFILIATE_LINK_REFERRER_POLICY = 'same-origin'

/**
 * Opens an affiliate/offer link in a new tab with the same rel and referrer
 * policy as the anchors (window.open cannot set a referrer policy).
 */
export const openAffiliateLinkInNewTab = (href: string): void => {
  const anchor = document.createElement('a')
  anchor.href = href
  anchor.target = '_blank'
  anchor.rel = AFFILIATE_LINK_REL
  anchor.referrerPolicy = AFFILIATE_LINK_REFERRER_POLICY
  anchor.style.display = 'none'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
}

export const resolveOfferHref = (offer: {
  affiliationToken?: string | null
  url?: string | null
}): string | undefined =>
  offer.affiliationToken
    ? `/contrib/${offer.affiliationToken}`
    : (offer.url ?? undefined)

/** Single-sourced condition color grammar, used everywhere a "new" vs
 * "occasion" offer badge/chip renders. */
export const NEW_CONDITION_COLOR = 'success'
export const OCCASION_CONDITION_COLOR = 'secondary'
