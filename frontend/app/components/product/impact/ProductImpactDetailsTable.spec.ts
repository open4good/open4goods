import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import ProductImpactDetailsTable from './ProductImpactDetailsTable.vue'
import type { ScoreView } from './impact-types'

describe('ProductImpactDetailsTable', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            detailsTitle: 'Assessment details',
            tableHeaders: {
              score: 'Indicator',
              value: 'Value',
              ranking: 'Rank',
            },
          },
        },
      },
    },
  })

  const scores: ScoreView[] = [
    { id: 'ECOSCORE', label: 'Eco', relativeValue: 4.5 },
    { id: 'CO2', label: 'CO2', relativeValue: 3.1, ranking: 12 },
  ]

  it('renders a table without the percentile column', () => {
    const wrapper = mount(ProductImpactDetailsTable, {
      props: { scores },
      global: {
        plugins: [i18n],
        stubs: {
          'v-table': defineComponent({
            name: 'VTableStub',
            setup(_, { slots }) {
              return () => h('table', { class: 'v-table-stub' }, slots.default?.())
            },
          }),
        },
      },
    })

    const headers = wrapper.findAll('th').map((node) => node.text())
    expect(headers).toContain('Indicator')
    expect(headers).toContain('Value')
    expect(headers).toContain('Rank')
    expect(headers).not.toContain('Percentile')
  })
})
