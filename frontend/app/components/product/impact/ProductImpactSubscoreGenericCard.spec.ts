import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import ProductImpactSubscoreGenericCard from './ProductImpactSubscoreGenericCard.vue'

import type { ScoreView } from './impact-types'

const VChipStub = defineComponent({
  name: 'VChipStub',
  setup(_props, { slots }) {
    return () => h('span', { class: 'v-chip-stub' }, slots.default?.())
  },
})

describe('ProductImpactSubscoreGenericCard', () => {
  // ... existing i18n setup ...

  //...

  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            percentile: 'Percentile',
            weightChip: 'Counts for {value}% of the total score',
            importanceTitle: 'Why it matters',
            tableHeaders: {
              ranking: 'Rank',
            },
            lifecycle: {
              USE: 'Use',
              END_OF_LIFE: 'End of life',
            },
            subscores: {
              default: {
                ranking: 'Rank {ranking} out of {count}.',
                readIndicator: {
                  title: 'How to read this indicator',
                  higher: {
                    worst: 'Worst indicator is {worst}.',
                    best: 'Best indicator is {best} across {count} {verticalTitle}.',
                    average:
                      'Average is {average}, equivalent to {averageOn20}/20.',
                    product: '{productName} scores {productOn20}/20.',
                  },
                  lower: {
                    worst: 'Highest indicator is {worst}.',
                    best: 'Lowest indicator is {best} across {count} {verticalTitle}.',
                    average:
                      'Average is {average}, equivalent to {averageOn20}/20.',
                    product: '{productName} scores {productOn20}/20.',
                  },
                },
              },
              repairability_index: {
                ranking: 'Rank {ranking} out of {count}.',
                readIndicator: {
                  title: 'Repairability indicator guidance',
                  higher: {
                    worst: 'Worst repairability indicator is {worst}.',
                    best: 'Best repairability indicator is {best} across {count} {verticalTitle}.',
                    average:
                      'Average repairability is {average}, equivalent to {averageOn20}/20.',
                    product: '{productName} repairability is {productOn20}/20.',
                  },
                  lower: {
                    worst: 'Highest repairability indicator is {worst}.',
                    best: 'Lowest repairability indicator is {best} across {count} {verticalTitle}.',
                    average:
                      'Average repairability is {average}, equivalent to {averageOn20}/20.',
                    product: '{productName} repairability is {productOn20}/20.',
                  },
                },
              },
            },
          },
        },
      },
    },
  })

  const score: ScoreView = {
    id: 'CO2',
    label: 'CO2 emissions',
    description: 'Lower is better',
    relativeValue: 3.6,
    absoluteValue: 42.5,
    absolute: { min: 10, max: 100, avg: 55, count: 200, value: 42.5 },
    energyClassDisplay: 'A',
    distribution: [
      { label: '1', value: 5 },
      { label: '2', value: 10 },
    ],
    coefficient: 0.42,
    ranking: 3,
    on20: 17.3,
    metadatas: {
      coverage: 'High',
    },
  }

  it('renders the header, value summary, chart and explanation details', () => {
    const wrapper = mount(ProductImpactSubscoreGenericCard, {
      props: {
        score,
        productName: 'Demo Product',
        productBrand: 'EcoCorp',
        productModel: 'Air 2000',
        productImage: '/cover.png',
        verticalTitle: 'televisions',
      },
      global: {
        plugins: [i18n],
        stubs: {
          ProductImpactSubscoreChart: true,
          ImpactCoefficientBadge: defineComponent({
            name: 'ImpactCoefficientBadgeStub',
            props: [
              'value',
              'labelKey',
              'labelParams',
              'tooltipKey',
              'tooltipParams',
            ],
            setup(props) {
              return () =>
                h(
                  'span',
                  { class: 'coefficient-stub' },
                  `coefficient:${props.value}`
                )
            },
          }),
          ClientOnly: defineComponent({
            name: 'ClientOnlyStub',
            setup(_, { slots }) {
              return () => slots.default?.()
            },
          }),
        },
      },
    })

    expect(wrapper.text()).toContain('coefficient:0.42')
    expect(wrapper.text()).toContain('17.3')
    expect(wrapper.find('.impact-subscore__value-number').text()).toBe('42.5')
    expect(wrapper.text()).toContain('A')
    expect(wrapper.text()).toContain('How to read this indicator')
    expect(wrapper.text()).toContain('Lower is better')
    expect(wrapper.text()).not.toContain('Percentile')
    expect(wrapper.text()).toContain('Rank')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('High')
    expect(wrapper.text()).not.toContain('Rank 3 out of 200.')
  })

  it('uses specialised translations when available', () => {
    const specializedScore: ScoreView = {
      ...score,
      id: 'REPAIRABILITY_INDEX',
      label: 'Repairability index',
    }

    const wrapper = mount(ProductImpactSubscoreGenericCard, {
      props: {
        score: specializedScore,
        productName: 'Demo Product',
        productBrand: 'EcoCorp',
        productModel: 'Air 2000',
        productImage: '/cover.png',
        verticalTitle: 'televisions',
      },
      global: {
        plugins: [i18n],
        stubs: {
          ProductImpactSubscoreChart: true,
          ImpactCoefficientBadge: defineComponent({
            name: 'ImpactCoefficientBadgeStub',
            props: [
              'value',
              'labelKey',
              'labelParams',
              'tooltipKey',
              'tooltipParams',
            ],
            setup(props) {
              return () =>
                h(
                  'span',
                  { class: 'coefficient-stub' },
                  `coefficient:${props.value}`
                )
            },
          }),
          ClientOnly: defineComponent({
            name: 'ClientOnlyStub',
            setup(_, { slots }) {
              return () => slots.default?.()
            },
          }),
        },
      },
    })

    expect(wrapper.text()).toContain('Repairability indicator guidance')
  })

  it('renders lifecycle badges in the header when available', () => {
    const scoreWithLifecycle: ScoreView = {
      ...score,
      participateInACV: ['USE', 'end_of_life'],
    }

    const wrapper = mount(ProductImpactSubscoreGenericCard, {
      props: {
        score: scoreWithLifecycle,
        productName: 'Demo Product',
        productBrand: 'EcoCorp',
        productModel: 'Air 2000',
        productImage: '/cover.png',
        verticalTitle: 'televisions',
      },
      global: {
        plugins: [i18n],
        stubs: {
          ProductImpactSubscoreChart: true,
          ImpactCoefficientBadge: true,
          ClientOnly: true,
          'v-chip': VChipStub,
        },
      },
    })

    const header = wrapper.find('.impact-subscore-header')
    expect(header.exists()).toBe(true)
    expect(header.text()).toContain('Use')
    expect(header.text()).toContain('end_of_life')
  })
})
