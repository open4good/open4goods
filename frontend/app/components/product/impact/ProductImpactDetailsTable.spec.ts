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
              coefficient: 'Coefficient',
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
    { id: 'CO2', label: 'CO2', relativeValue: 3.1, value: 2.8, ranking: 12, coefficient: 0.3 },
  ]

  it('renders details without the ranking column and hides ecoscore', () => {
    const wrapper = mount(ProductImpactDetailsTable, {
      props: { scores },
      global: {
        plugins: [i18n],
        stubs: {
          'v-data-table': defineComponent({
            name: 'VDataTableStub',
            props: ['headers', 'items'],
            setup(props, { slots }) {
              return () =>
                h('table', { class: 'v-data-table-stub' }, [
                  h(
                    'thead',
                    h(
                      'tr',
                      (props.headers as Array<{ title: string; key: string }>)?.map((header) =>
                        h('th', header.title),
                      ) ?? [],
                    ),
                  ),
                  h(
                    'tbody',
                    (props.items as Array<Record<string, unknown>>)?.map((item) =>
                      h('tr', [
                        h(
                          'td',
                          slots['item.label']?.({ value: item.label, item }) ?? String(item.label ?? ''),
                        ),
                        h(
                          'td',
                          slots['item.displayValue']?.({ value: item.displayValue, item })
                            ?? String(item.displayValue ?? ''),
                        ),
                        h(
                          'td',
                          slots['item.coefficient']?.({ value: item.coefficient, item })
                            ?? String(item.coefficient ?? ''),
                        ),
                      ]),
                    ) ?? [],
                  ),
                ])
            },
          }),
          ProductImpactSubscoreRating: defineComponent({
            name: 'ProductImpactSubscoreRatingStub',
            props: ['score', 'max', 'size', 'showValue'],
            setup(props) {
              return () => h('div', { class: 'subscore-rating-stub' }, `rating:${props.score}`)
            },
          }),
          ImpactCoefficientBadge: defineComponent({
            name: 'ImpactCoefficientBadgeStub',
            props: ['value', 'labelKey', 'labelParams', 'tooltipKey', 'tooltipParams'],
            setup(props) {
              return () => h('span', { class: 'coefficient-stub' }, `coefficient:${props.value}`)
            },
          }),
        },
      },
    })

    const headers = wrapper.findAll('th').map((node) => node.text())
    expect(headers).toContain('Indicator')
    expect(headers).toContain('Value')
    expect(headers).toContain('Coefficient')

    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(1)
    const rowText = rows[0]?.text() ?? ''
    expect(rowText).toContain('CO2')
    expect(rowText).toContain('rating:3.1')
    expect(rowText).toContain('3.1 / 5')
    expect(rowText).toContain('coefficient:0.3')
    expect(wrapper.text()).not.toContain('Eco')
  })

  it('shows a fallback message when no rows remain', () => {
    const wrapper = mount(ProductImpactDetailsTable, {
      props: { scores: [{ id: 'ECOSCORE', label: 'Eco', relativeValue: 4.2 }] },
      global: {
        plugins: [i18n],
        stubs: {
          'v-data-table': defineComponent({
            name: 'VDataTableStub',
            props: ['headers', 'items'],
            setup(_, { slots }) {
              return () => h('table', { class: 'v-data-table-stub' }, slots.default?.())
            },
          }),
          ProductImpactSubscoreRating: defineComponent({
            name: 'ProductImpactSubscoreRatingStub',
            props: ['score', 'max', 'size', 'showValue'],
            setup(props) {
              return () => h('div', { class: 'subscore-rating-stub' }, `rating:${props.score}`)
            },
          }),
          ImpactCoefficientBadge: defineComponent({
            name: 'ImpactCoefficientBadgeStub',
            props: ['value', 'labelKey', 'labelParams', 'tooltipKey', 'tooltipParams'],
            setup(props) {
              return () => h('span', { class: 'coefficient-stub' }, `coefficient:${props.value}`)
            },
          }),
        },
      },
    })

    expect(wrapper.text()).toContain('No details available')
  })
})
