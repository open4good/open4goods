import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import { createI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import ProductAlternativeCard from './ProductAlternativeCard.vue'

vi.mock('~/stores/useProductCompareStore', () => ({
  useProductCompareStore: () => ({
    hasProduct: vi.fn().mockReturnValue(false),
    canAddProduct: vi.fn().mockReturnValue({ success: true }),
    toggleProduct: vi.fn(),
  }),
  MAX_COMPARE_ITEMS: 4,
}))

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
          hero: {
            compare: {
              add: 'Add to compare',
              remove: 'Remove from compare',
              label: 'Compare',
              selected: 'In comparison',
              ariaAdd: 'Add {name} to compare',
              ariaSelected: '{name} already in compare',
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
          'v-tooltip': defineComponent({
            name: 'VTooltipStub',
            props: ['text'],
            setup(_, { slots }) {
              return () => slots.activator?.({ props: {} }) ?? slots.default?.() ?? h('div')
            },
          }),
          'v-icon': defineComponent({
            name: 'VIconStub',
            props: ['icon', 'size'],
            template: '<span class="v-icon-stub"></span>',
          }),
          'v-btn': defineComponent({
            name: 'VBtnStub',
            props: ['variant', 'color', 'ariaPressed', 'ariaLabel', 'title', 'disabled'],
            setup(_, { slots }) {
              return () => h('button', { class: 'v-btn-stub', type: 'button' }, slots.default?.())
            },
          }),
        },
      },
    })

    expect(wrapper.find('.product-alternative-card__title').text()).toBe('EcoCorp • X')
    expect(wrapper.find('.product-alternative-card__attributes').exists()).toBe(false)

    const priceText = wrapper.find('.product-alternative-card__price').text()
    expect(priceText).toMatch(/299/)

    expect(wrapper.find('.product-alternative-card__compare-btn').exists()).toBe(true)
  })
})
