import { mountSuspended } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import ProductPriceRows from './ProductPriceRows.vue'
import type { ProductDto } from '~~/shared/api-client'
import { AFFILIATE_LINK_REL } from '~/utils/_product-pricing'

const trackAffiliateClickMock = vi.hoisted(() => vi.fn())
const trackProductRedirectMock = vi.hoisted(() => vi.fn())

vi.mock('~/composables/useAnalytics', () => ({
  useAnalytics: () => ({
    extractTokenFromLink: (link?: string | null) =>
      link?.split('/').filter(Boolean).pop() ?? null,
    isClientContribLink: (link?: string | null) =>
      Boolean(link?.startsWith('/contrib/')),
    trackAffiliateClick: trackAffiliateClickMock,
    trackProductRedirect: trackProductRedirectMock,
  }),
}))

const i18nMessages = {
  'fr-FR': {
    category: {
      products: {
        pricing: {
          newOfferLabel: 'Neuf',
          occasionOfferLabel: 'Occasion',
          bestOfferLabel: 'Meilleur prix',
        },
      },
    },
    product: {
      price: {
        trend: {
          decrease: 'Baisse de {amount}',
          increase: 'Hausse de {amount}',
          stable: 'Prix stable',
        },
      },
      hero: {
        trendPeriodDays: 'depuis {count} jours',
        trendTooltip: '{deviation} {period}',
      },
    },
  },
}

describe('ProductPriceRows', () => {
  const baseProduct: ProductDto = {
    id: '123',
    offers: {
      offersCount: 1,
      bestPrice: {
        price: 100,
        currency: 'EUR',
      },
      offersByCondition: {},
    },
  }

  const mountComponent = async (product: ProductDto) => {
    const i18n = createI18n({
      legacy: false,
      locale: 'fr-FR',
      messages: i18nMessages,
    })

    return mountSuspended(ProductPriceRows, {
      props: {
        product,
      },
      global: {
        plugins: [[i18n]],
      },
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders a decrease trend correctly', async () => {
    const product: ProductDto = {
      ...baseProduct,
      offers: {
        ...baseProduct.offers,
        bestNewOffer: {
          price: 90,
          currency: 'EUR',
          merchantName: 'Test Merchant',
        },
        newTrend: {
          trend: 'PRICE_DECREASE',
          variation: -10,
          period: 86400000 * 2, // 2 days
        },
        offersByCondition: {
          NEW: [{ price: 90, currency: 'EUR', condition: 'NEW' }],
        },
      },
    }

    const wrapper = await mountComponent(product)

    // Check if the trend icon exists and has the correct props
    const trendIconComp = wrapper
      .findAllComponents({ name: 'VIcon' })
      .find(c => c.attributes('class')?.includes('product-price-rows__trend'))

    expect(trendIconComp?.props('color')).toBe('success')
    expect(trendIconComp?.props('icon')).toBe('mdi-trending-down')
  })

  it('renders the offer link with sponsored rel and a /contrib/ href when a token exists', async () => {
    const product: ProductDto = {
      ...baseProduct,
      offers: {
        ...baseProduct.offers,
        bestNewOffer: {
          price: 90,
          currency: 'EUR',
          merchantName: 'Test Merchant',
          url: 'https://merchant.example/offer',
          affiliationToken: 'tok-123',
        },
        offersByCondition: {
          NEW: [{ price: 90, currency: 'EUR', condition: 'NEW' }],
        },
      },
    }

    const wrapper = await mountComponent(product)
    const link = wrapper.find('a.product-price-rows__content')

    expect(link.attributes('href')).toBe('/contrib/tok-123')
    expect(link.attributes('rel')).toBe(AFFILIATE_LINK_REL)
    expect(link.attributes('target')).toBe('_blank')
  })

  it('tracks affiliate clicks before opening a contrib link', async () => {
    const product: ProductDto = {
      ...baseProduct,
      gtin: 1234567890123,
      fullSlug: 'televisions/test-product',
      base: { vertical: 'televisions' },
      offers: {
        ...baseProduct.offers,
        bestNewOffer: {
          price: 90,
          currency: 'EUR',
          datasourceName: 'Test Merchant',
          affiliationToken: 'tok-123',
        },
        offersByCondition: {},
      },
    }
    const wrapper = await mountComponent(product)

    await wrapper.find('a.product-price-rows__content').trigger('click')

    expect(trackProductRedirectMock).toHaveBeenCalledWith({
      token: 'tok-123',
      placement: 'price-row',
      source: 'Test Merchant',
      url: '/contrib/tok-123',
    })
    expect(trackAffiliateClickMock).toHaveBeenCalledWith(
      expect.objectContaining({
        token: 'tok-123',
        placement: 'price-row',
        productId: 1234567890123,
        categorySlug: 'televisions',
      })
    )
  })
})
