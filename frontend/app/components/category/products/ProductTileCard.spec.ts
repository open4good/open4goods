import { describe, expect, it, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createVuetify } from 'vuetify'
import { createI18n } from 'vue-i18n'
import { createPinia, setActivePinia } from 'pinia'
import type { ProductDto } from '~~/shared/api-client'
import ProductTileCard from './ProductTileCard.vue'
import { useProductCompareStore } from '~/stores/useProductCompareStore'

const vuetify = createVuetify()
const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      category: {
        products: {
          compare: {
            added: 'Added',
            buttonLabel: 'Compare',
          },
        },
      },
    },
  },
})

const createWrapper = (overrides: Partial<ProductDto> = {}) => {
  const product: ProductDto = {
    gtin: overrides.gtin ?? 123,
    identity: {
      brand: overrides.identity?.brand ?? 'Acme',
      model: overrides.identity?.model ?? 'Model Z',
      bestName: overrides.identity?.bestName ?? 'Acme Model Z',
    },
    base: overrides.base,
    resources: overrides.resources,
    offers: overrides.offers,
    names: overrides.names,
    scores: overrides.scores,
    attributes: overrides.attributes,
    datasources: overrides.datasources,
    aiReview: overrides.aiReview,
    slug: overrides.slug,
    fullSlug: overrides.fullSlug,
  }

  return mount(ProductTileCard, {
    props: {
      product,
      productLink: '/products/acme',
      imageSrc: '/image.jpg',
      attributes: [
        { key: 'color', label: 'Color', value: 'Blue', icon: 'mdi-palette' },
        { key: 'size', label: 'Size', value: 'Large' },
      ],
      impactScore: 4,
      offerBadges: [
        { key: 'new', label: 'New', price: '99', appearance: 'new' },
        { key: 'used', label: 'Used', price: '29', appearance: 'occasion' },
      ],
      offersCountLabel: '3 offers',
      untitledLabel: 'Untitled',
      notRatedLabel: 'Not rated',
    },
    global: {
      plugins: [vuetify, i18n],
      stubs: {
        NuxtLink: false,
        VImg: true,
        VSkeletonLoader: true,
        VChip: true,
        VIcon: true,
        VBtn: true,
        ImpactScore: true,
      },
    },
  })
}

describe('ProductTileCard', () => {
  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)
  })

  it('builds the header title from brand and attributes', () => {
    const wrapper = createWrapper()

    expect(wrapper.find('.product-tile-card__title').text()).toBe('Acme')
    expect(wrapper.find('.product-tile-card__subtitle').text()).toBe('Model Z')
  })

  it('renders attribute chips and pricing badges', () => {
    const wrapper = createWrapper()

    expect(wrapper.findAll('.product-tile-card__attribute')).toHaveLength(2)
    expect(wrapper.findAll('.product-tile-card__price-badge')).toHaveLength(2)
  })

  it('toggles compare state via the store', async () => {
    const wrapper = createWrapper()
    const store = useProductCompareStore()

    expect(store.hasProduct(wrapper.props('product'))).toBe(false)

    await wrapper.find('[data-test="product-tile-compare"]').trigger('click')

    expect(store.hasProduct(wrapper.props('product'))).toBe(true)
  })
})
