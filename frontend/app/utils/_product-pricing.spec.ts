import { describe, expect, it } from 'vitest'
import { formatBestPrice, formatPrice, resolveOfferHref } from './_product-pricing'
import type { ProductDto } from '~~/shared/api-client'

const frNumberFormatter = (
  value: number,
  options?: Intl.NumberFormatOptions
) => new Intl.NumberFormat('fr-FR', options).format(value)

describe('formatPrice', () => {
  it('formats a EUR price using French locale conventions (comma decimal, no dot)', () => {
    const result = formatPrice(1890, frNumberFormatter, 'EUR')

    expect(result).not.toMatch(/\d\.\d/)
    expect(result).toContain('890')
  })

  it('formats a non-round price with two decimal digits, comma separated', () => {
    const result = formatPrice(433.2, frNumberFormatter, 'EUR')

    expect(result).not.toMatch(/\d\.\d/)
    expect(result).toMatch(/433,2/)
  })

  it('falls back to a plain localized number when no currency is given', () => {
    const result = formatPrice(1890, frNumberFormatter)

    expect(result).not.toMatch(/\d\.\d/)
    expect(result).toMatch(/^1.890,00$/u)
  })
})

describe('formatBestPrice', () => {
  const translate = (key: string) => key

  it('renders the unavailable-price key when there is no best offer', () => {
    const product = { offers: {} } as ProductDto

    expect(formatBestPrice(product, translate, frNumberFormatter)).toBe(
      'category.products.priceUnavailable'
    )
  })

  it('formats the best offer price consistently with formatPrice', () => {
    const product = {
      offers: { bestPrice: { price: 999, currency: 'EUR' } },
    } as ProductDto

    const result = formatBestPrice(product, translate, frNumberFormatter)

    expect(result).not.toMatch(/\d\.\d/)
  })
})

describe('resolveOfferHref', () => {
  it('prefers the affiliation contrib redirect over the raw merchant url', () => {
    expect(
      resolveOfferHref({ affiliationToken: 'abc123', url: 'https://merchant.example' })
    ).toBe('/contrib/abc123')
  })

  it('falls back to the raw url when there is no affiliation token', () => {
    expect(resolveOfferHref({ url: 'https://merchant.example' })).toBe(
      'https://merchant.example'
    )
  })
})
