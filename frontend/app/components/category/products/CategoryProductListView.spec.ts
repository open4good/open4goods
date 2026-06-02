import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'

vi.mock('~/composables/usePluralizedTranslation', () => ({
  usePluralizedTranslation: () => ({
    translatePlural: (_key: string, count: number) => `${count}`,
  }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    n: (value: number) => String(value),
    locale: { value: 'en-US' },
  }),
}))

const VListStub = defineComponent({
  name: 'VList',
  setup(_, { attrs, slots }) {
    return () => h('div', attrs, slots.default?.())
  },
})

const VListItemStub = defineComponent({
  name: 'VListItem',
  props: {
    to: { type: String, default: undefined },
  },
  setup(props, { attrs, slots }) {
    return () =>
      h(
        'a',
        {
          ...attrs,
          href: props.to,
          'data-test': 'product-row',
        },
        [slots.prepend?.(), slots.default?.()]
      )
  },
})

const VAvatarStub = defineComponent({
  name: 'VAvatar',
  setup(_, { slots }) {
    return () => h('div', { 'data-test': 'product-avatar' }, slots.default?.())
  },
})

const VImgStub = defineComponent({
  name: 'VImg',
  props: {
    src: { type: String, default: undefined },
    alt: { type: String, default: '' },
  },
  setup(props) {
    return () =>
      h('img', {
        src: props.src,
        alt: props.alt,
        'data-test': 'product-image',
      })
  },
})

const mountList = async (products: ProductDto[]) => {
  const module = await import('./CategoryProductListView.vue')
  const CategoryProductListView = module.default
  const popularAttributes: AttributeConfigDto[] = [
    {
      key: 'DIAGONALE_POUCES',
      name: 'Screen size',
      suffix: '"',
      filteringType: 'NUMERIC',
    } as AttributeConfigDto,
  ]

  return mount(CategoryProductListView, {
    props: {
      products,
      popularAttributes,
      sortField: 'attributes.indexed.DIAGONALE_POUCES.numericValue',
      fieldMetadata: {
        'attributes.indexed.DIAGONALE_POUCES.numericValue': {
          mapping: 'attributes.indexed.DIAGONALE_POUCES.numericValue',
          title: 'Screen size',
        },
      },
    },
    global: {
      stubs: {
        VList: VListStub,
        VListItem: VListItemStub,
        VAvatar: VAvatarStub,
        VImg: VImgStub,
        VSkeletonLoader: true,
        ProductPriceRows: defineComponent({
          name: 'ProductPriceRows',
          setup() {
            return () => h('div', { 'data-test': 'price-rows' }, 'price')
          },
        }),
        ImpactScore: defineComponent({
          name: 'ImpactScore',
          setup() {
            return () => h('div', { 'data-test': 'impact-score' }, 'score')
          },
        }),
        CompareToggleButton: defineComponent({
          name: 'CompareToggleButton',
          props: {
            product: { type: Object, required: true },
          },
          setup() {
            return () =>
              h('button', {
                type: 'button',
                'data-test': 'product-compare-toggle',
              })
          },
        }),
      },
      mocks: {
        $t: (key: string) => key,
      },
    },
  })
}

const buildProduct = (overrides: Partial<ProductDto> = {}): ProductDto => ({
  gtin: overrides.gtin ?? 123,
  fullSlug: overrides.fullSlug ?? '/products/example',
  identity: {
    bestName: overrides.identity?.bestName ?? 'Example TV',
    brand: overrides.identity?.brand ?? 'Brand',
    model: overrides.identity?.model,
  },
  resources: overrides.resources ?? {
    coverImagePath: '/cover.jpg',
  },
  attributes: overrides.attributes ?? {
    indexedAttributes: {
      DIAGONALE_POUCES: {
        name: 'Screen size',
        value: '55',
        numericValue: 55,
      },
    },
  },
  aiReview: overrides.aiReview,
  scores: overrides.scores,
})

describe('CategoryProductListView', () => {
  it('renders dense product rows with link, image alt, sorted attribute, and compare toggle', async () => {
    const wrapper = await mountList([buildProduct()])

    expect(wrapper.get('[data-test="product-row"]').attributes('href')).toBe(
      '/products/example'
    )
    expect(wrapper.get('[data-test="product-image"]').attributes('alt')).toBe(
      'Example TV'
    )
    expect(wrapper.get('.category-product-list__attribute--sorted').text())
      .toContain('55 "')
    expect(wrapper.find('[data-test="product-compare-toggle"]').exists()).toBe(
      true
    )
  })

  it('renders the not-rated fallback when a product has no impact score', async () => {
    const wrapper = await mountList([buildProduct({ scores: undefined })])

    expect(wrapper.find('[data-test="impact-score"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('category.products.notRated')
  })
})
