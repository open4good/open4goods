import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import ProductPriceRows from './ProductPriceRows.vue'
import type { ProductDto } from '~~/shared/api-client'

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
})
