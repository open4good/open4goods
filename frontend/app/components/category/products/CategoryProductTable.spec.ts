import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createVuetify } from 'vuetify'
import { defineComponent, h } from 'vue'
import type { Component } from 'vue'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

vi.mock('~/composables/usePluralizedTranslation', () => ({
  usePluralizedTranslation: () => ({
    translatePlural: (_key: string, count: number) => `${count}`,
  }),
}))

const VDataTableStub = defineComponent({
  name: 'VDataTableStub',
  props: {
    headers: {
      type: Array,
      default: () => [],
    },
    items: {
      type: Array,
      default: () => [],
    },
    sortBy: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['update:sort-by', 'click:row'],
  setup(_props, { slots }) {
    return () => h('div', { 'data-test': 'data-table-stub' }, slots.default?.())
  },
})

const CompareToggleStub: Component = {
  inheritAttrs: false,
  props: {
    product: {
      type: Object,
      required: true,
    },
    size: {
      type: String,
      default: 'comfortable',
    },
  },
  setup() {
    return () =>
      h('button', { type: 'button', 'data-test': 'category-product-compare' })
  },
}

const mountTable = async (props?: Record<string, unknown>) => {
  const module = await import('./CategoryProductTable.vue')
  const CategoryProductTable = module.default

  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        category: {
          products: {
            headers: {
              compare: 'Compare',
              brand: 'Brand',
              model: 'Model',
              impactScore: 'Impact score',
              bestPrice: 'Best price',
              offers: 'Offers',
              popularAttributes: 'Highlights',
              remainingAttributes: 'Other attributes',
            },
            notRated: 'Not rated',
            priceUnavailable: 'Price unavailable',
            notRatedYet: 'Not rated yet',
          },
        },
        common: {
          boolean: {
            true: 'Yes',
            false: 'No',
          },
        },
      },
    },
  })

  const vuetify = createVuetify()

  return mount(CategoryProductTable, {
    props: {
      products: [],
      itemsPerPage: 20,
      popularAttributes: [],
      sortField: null,
      sortOrder: 'desc',
      attributeKeys: [],
      attributeConfigs: {},
      fieldMetadata: {},
      ...props,
    },
    global: {
      plugins: [i18n, vuetify],
      stubs: {
        VDataTable: VDataTableStub,
        ImpactScore: defineComponent({
          setup(_, { slots }) {
            return () =>
              h('div', { 'data-test': 'impact-score-stub' }, slots.default?.())
          },
        }),
        CategoryProductCompareToggle: CompareToggleStub,
      },
    },
  })
}

const buildProduct = (overrides: Partial<ProductDto> = {}): ProductDto => ({
  gtin: overrides.gtin ?? 1,
  identity: {
    bestName: overrides.identity?.bestName ?? 'Example product',
    model: overrides.identity?.model,
    brand: overrides.identity?.brand ?? 'Brand',
  },
  resources: overrides.resources,
  offers: overrides.offers ?? {
    offersCount: 2,
    bestPrice: {
      price: 499,
      currency: 'EUR',
    },
  },
  attributes: overrides.attributes ?? {
    indexedAttributes: {
      DIAGONALE_POUCES: {
        name: 'Diagonale',
        value: '32',
        numericValue: 32,
      },
    },
  },
  scores: overrides.scores,
  base: overrides.base,
  slug: overrides.slug,
  fullSlug: overrides.fullSlug,
})

describe('CategoryProductTable', () => {
  let attributeConfigs: Record<string, AttributeConfigDto>

  beforeEach(() => {
    attributeConfigs = {
      DIAGONALE_POUCES: {
        key: 'DIAGONALE_POUCES',
        name: 'Screen size',
        suffix: '"',
        filteringType: 'NUMERIC',
      } as AttributeConfigDto,
    }
  })

  it('renders attribute columns with formatted values', async () => {
    const product = buildProduct()

    const wrapper = await mountTable({
      products: [product],
      attributeKeys: ['DIAGONALE_POUCES'],
      attributeConfigs,
      fieldMetadata: {
        'attributes.indexed.DIAGONALE_POUCES.numericValue': {
          mapping: 'attributes.indexed.DIAGONALE_POUCES.numericValue',
          title: 'Screen size',
        },
      },
    })

    const table = wrapper.getComponent(VDataTableStub)
    const headers = table.props('headers') as Array<{ key: string }>
    const items = table.props('items') as Array<Record<string, unknown>>

    expect(
      headers.some(header => header.key === 'attribute:DIAGONALE_POUCES')
    ).toBe(true)
    expect(items[0]!['attribute:DIAGONALE_POUCES']).toBe('32 "')
  })

  it('emits sort updates for attribute columns', async () => {
    const wrapper = await mountTable({
      products: [buildProduct()],
      attributeKeys: ['DIAGONALE_POUCES'],
      attributeConfigs,
      fieldMetadata: {
        'attributes.indexed.DIAGONALE_POUCES.numericValue': {
          mapping: 'attributes.indexed.DIAGONALE_POUCES.numericValue',
          title: 'Screen size',
        },
      },
    })

    const table = wrapper.getComponent(VDataTableStub)
    table.vm.$emit('update:sort-by', [
      { key: 'attribute:DIAGONALE_POUCES', order: 'asc' },
    ])

    expect(wrapper.emitted('update:sort-field')?.[0]?.[0]).toBe(
      'attributes.indexed.DIAGONALE_POUCES.numericValue'
    )
    expect(wrapper.emitted('update:sort-order')?.[0]?.[0]).toBe('asc')
  })

  it('emits sort updates for static sortable columns', async () => {
    const wrapper = await mountTable({
      products: [buildProduct()],
    })

    const table = wrapper.getComponent(VDataTableStub)

    table.vm.$emit('update:sort-by', [{ key: 'brand', order: 'asc' }])
    expect(wrapper.emitted('update:sort-field')?.[0]?.[0]).toBe(
      'attributes.referentielAttributes.BRAND'
    )
    expect(wrapper.emitted('update:sort-order')?.[0]?.[0]).toBe('asc')

    table.vm.$emit('update:sort-by', [{ key: 'model', order: 'desc' }])
    expect(wrapper.emitted('update:sort-field')?.[1]?.[0]).toBe(
      'attributes.referentielAttributes.MODEL'
    )
    expect(wrapper.emitted('update:sort-order')?.[1]?.[0]).toBe('desc')

    table.vm.$emit('update:sort-by', [{ key: 'impactScore' }])
    expect(wrapper.emitted('update:sort-field')?.[2]?.[0]).toBe(
      ECOSCORE_RELATIVE_FIELD
    )
    expect(wrapper.emitted('update:sort-order')?.[2]?.[0]).toBe('asc')
  })
})
