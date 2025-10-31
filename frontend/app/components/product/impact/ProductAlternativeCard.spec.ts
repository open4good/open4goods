import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { defineComponent } from 'vue'
import { createI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import ProductAlternativeCard from './ProductAlternativeCard.vue'

describe('ProductAlternativeCard', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            alternatives: {
              untitled: 'Untitled product',
              viewProduct: 'View product {name}',
            },
          },
        },
        category: {
          products: {
            priceUnavailable: 'Price unavailable',
            compare: {
              addToList: 'Add to compare',
              removeFromList: 'Remove from compare',
              limitReached: 'Limit reached',
              differentCategory: 'Different category',
              missingIdentifier: 'Missing identifier',
            },
            offerCount: '{count} offers',
            untitledProduct: 'Unnamed product',
          },
        },
      },
    },
  })

  it('renders product information, price and impact score', async () => {
    const product: ProductDto = {
      gtin: 1234567890,
      slug: 'eco-phone-x',
      fullSlug: '/phones/eco-phone-x',
      base: {
        ecoscoreValue: 14,
      },
      identity: {
        brand: 'EcoCorp',
        model: 'X',
        bestName: 'EcoCorp X',
      },
      offers: {
        bestPrice: {
          price: 299.99,
          currency: 'EUR',
        },
      },
      resources: {
        coverImagePath: '/images/eco-phone-x.webp',
      },
      scores: {
        ecoscore: {
          on20: 16,
        },
        scores: {
          ECOSCORE: {
            on20: 16,
          },
        },
      },
    } as unknown as ProductDto

    const wrapper = await mountSuspended(ProductAlternativeCard, {
      props: { product },
      global: {
        plugins: [i18n],
        stubs: {
          NuxtImg: {
            template: '<img />',
          },
          ImpactScore: defineComponent({
            name: 'ImpactScoreStub',
            template: '<div class="impact-score-stub"></div>',
          }),
          CategoryProductCompareToggle: defineComponent({
            name: 'CategoryProductCompareToggleStub',
            template: '<div data-test="category-product-compare"></div>',
          }),
        },
      },
    })

    expect(wrapper.find('.product-alternative-card__title').text()).toBe('EcoCorp X')
    expect(wrapper.find('.product-alternative-card__subtitle').text()).toContain('EcoCorp')

    const priceText = wrapper.find('.product-alternative-card__price').text()
    expect(priceText).toMatch(/299/)

    expect(wrapper.find('.impact-score-stub').exists()).toBe(true)
    expect(wrapper.find('[data-test="category-product-compare"]').exists()).toBe(true)
  })
})
