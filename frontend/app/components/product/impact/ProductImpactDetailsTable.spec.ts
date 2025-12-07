import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import ProductImpactDetailsTable from './ProductImpactDetailsTable.vue'
import type { ScoreView } from './impact-types'

vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({
    currentCategory: ref({ attributesConfig: { configs: [] } }),
  }),
}))

const createStub = (tag: string, className?: string) =>
  defineComponent({
    name: `${tag}-stub`,
    inheritAttrs: false,
    setup(_, { slots, attrs }) {
      return () => h(tag, { class: className ?? `${tag}-stub`, ...attrs }, slots.default?.())
    },
  })

const VDataTableStub = defineComponent({
  name: 'VDataTableStub',
  props: {
    headers: Array,
    items: Array,
  },
  setup(props, { slots }) {
    return () =>
      h('table', { class: 'v-data-table-stub' }, [
        h(
          'tbody',
          {},
          (props.items as unknown[] | undefined)?.map((row, index) =>
            slots.item?.({ item: { raw: row, index }, columns: props.headers ?? [] }),
          ),
        ),
      ])
  },
})

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  messages: {
    'en-US': {
      product: {
        impact: {
          detailsTitle: 'Impact details',
          hideDetails: 'Hide details',
          subscoreDetailsToggle: 'Show details',
          noDetailsAvailable: 'No details',
          tableHeaders: {
            scoreName: 'Score',
            attributeValue: 'Attribute',
            scoreValue: 'Value',
            coefficient: 'Coefficient',
            lifecycle: 'Lifecycle',
          },
          aggregateScores: {
            TOTAL: 'Overall Impact',
          },
          lifecycle: {
            EXTRACTION: 'Extraction',
            MANUFACTURING: 'Manufacturing',
            TRANSPORTATION: 'Transportation',
            USE: 'Use',
            END_OF_LIFE: 'End of life',
          },
          valueOutOf: '{value} / {max}',
        },
      },
    },
  },
})

describe('ProductImpactDetailsTable', () => {
  const scores: Array<ScoreView | undefined> = [
    {
      id: 'TOTAL',
      label: 'Overall Impact',
      aggregates: { POWER: true },
      relativeValue: 4.2,
      coefficient: 1,
    },
    undefined,
    {
      id: 'POWER',
      label: 'Energy Efficiency',
      attributeValue: '45',
      attributeSuffix: 'W',
      relativeValue: 3.6,
      coefficient: 0.5,
    },
    {
      id: 'REPAIR',
      label: 'Repairability',
      relativeValue: 2.1,
      coefficient: 0.3,
    },
  ]

  it('renders valid rows even when some scores are undefined', () => {
    const wrapper = mount(ProductImpactDetailsTable, {
      props: { scores: scores as ScoreView[] },
      global: {
        plugins: [i18n],
        stubs: {
          'v-data-table': VDataTableStub,
          'v-btn': createStub('button', 'v-btn-stub'),
          'v-icon': createStub('span', 'v-icon-stub'),
          'v-chip': createStub('span', 'v-chip-stub'),
          ProductAttributeSourcingLabel: createStub('div', 'attribute-stub'),
          ImpactCoefficientBadge: createStub('div', 'coefficient-stub'),
          ProductImpactSubscoreRating: createStub('div', 'rating-stub'),
        },
      },
    })

    expect(wrapper.findAll('tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('Overall Impact')
    expect(wrapper.text()).toContain('Repairability')
  })
})
