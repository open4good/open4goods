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
            },
            noDetailsAvailable: 'No details available',
            valueOutOf: '{value} / {max}',
          },
        },
      },
    },
  })

  const scores: ScoreView[] = [
    { id: 'ECOSCORE', label: 'Eco', relativeValue: 4.5 },
    { id: 'CO2', label: 'CO2', relativeValue: 3.1, value: 2.8, ranking: 12 },
  ]

  it('renders details without the ranking column and hides ecoscore', () => {
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
          ProductImpactSubscoreRating: defineComponent({
            name: 'ProductImpactSubscoreRatingStub',
            props: ['score', 'max', 'size', 'showValue'],
            setup(props) {
              return () => h('div', { class: 'subscore-rating-stub' }, `rating:${props.score}`)
            },
          }),
        },
      },
    })

    const headers = wrapper.findAll('th').map((node) => node.text())
    expect(headers).toContain('Indicator')
    expect(headers).toContain('Value')
    expect(headers).not.toContain('Rank')

    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(1)
    expect(rows[0]?.text()).toContain('CO2')
    expect(rows[0]?.text()).toContain('rating:2.8')
    expect(rows[0]?.text()).toContain('2.8 / 5')
    expect(wrapper.text()).not.toContain('Eco')
  })

  it('shows a fallback message when no rows remain', () => {
    const wrapper = mount(ProductImpactDetailsTable, {
      props: { scores: [{ id: 'ECOSCORE', label: 'Eco', relativeValue: 4.2 }] },
      global: {
        plugins: [i18n],
        stubs: {
          'v-table': defineComponent({
            name: 'VTableStub',
            setup(_, { slots }) {
              return () => h('table', { class: 'v-table-stub' }, slots.default?.())
            },
          }),
          ProductImpactSubscoreRating: defineComponent({
            name: 'ProductImpactSubscoreRatingStub',
            props: ['score', 'max', 'size', 'showValue'],
            setup(props) {
              return () => h('div', { class: 'subscore-rating-stub' }, `rating:${props.score}`)
            },
          }),
        },
      },
    })

    expect(wrapper.text()).toContain('No details available')
  })
})
