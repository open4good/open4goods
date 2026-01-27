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
      products: {
        compare: {
          addToList: 'Add to compare',
          removeFromList: 'Remove from compare',
          removeSingle: 'Remove {name}',
          limitReached: 'Limit reached',
          differentCategory: 'Different category',
          missingIdentifier: 'Missing identifier',
          addButtonShort: 'Compare',
          addedButtonShort: 'Added',
          addButtonFull: 'Add to compare',
          removeButtonFull: 'Remove from compare',
        },
      },
      category: {
        products: {
          untitledProduct: 'Untitled Product',
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
        VTooltip: {
          template: '<div><slot name="activator" :props="{}" /></div>',
        },
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

  it('builds the header title from the best available name', () => {
    const wrapper = createWrapper({ names: { prettyName: 'Pretty Z' } })

    expect(wrapper.find('.product-tile-card__title').text()).toBe('Pretty Z')
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

    await wrapper.find('[data-test="product-compare-toggle"]').trigger('click')

    expect(store.hasProduct(wrapper.props('product'))).toBe(true)
  })
})
