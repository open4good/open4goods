import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import ProductImpactSubscoreRepairabilityIndexCard from './ProductImpactSubscoreRepairabilityIndexCard.vue'
import type { ScoreView } from '../impact-types'

describe('ProductImpactSubscoreRepairabilityIndexCard', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            weightChip: 'Counts for {value}% of the total score',
            weightTooltip:
              'The {scoreName} score counts for {value}% of the total score',
            subscoreDetailsToggle: 'View indicator details',
            tableHeaders: {
              ranking: 'Rank',
            },
            subscores: {
              default: {
                ranking: 'Rank {ranking} out of {count}.',
                readIndicator: {
                  title: 'How to read this indicator',
                  worst: 'Worst indicator is {worst}.',
                  best: 'Best indicator is {best} across {count} {verticalTitle}.',
                  average:
                    'Average is {average}, equivalent to {averageOn20}/20.',
                  product: '{productName} scores {productOn20}/20.',
                },
              },
            },
          },
        },
      },
    },
  })

  const baseScore: ScoreView = {
    id: 'REPAIRABILITY_INDEX',
    label: 'Repairability index',
    description: null,
    relativeValue: 6.4,
    absoluteValue: 6.4,
    absolute: { value: 6.37, min: 0.5, max: 9.5, avg: 5.5, count: 120 },
    coefficient: 0.2,
    ranking: 12,
    on20: 15.6,
    distribution: [],
    energyLetter: null,
    metadatas: null,
  }

  it('renders the repairability illustration associated with the score', () => {
    const wrapper = mount(ProductImpactSubscoreRepairabilityIndexCard, {
      props: {
        score: baseScore,
        productName: 'Demo Product',
        productBrand: 'EcoCorp',
        productModel: 'Air 2000',
        productImage: '/cover.png',
        verticalTitle: 'televisions',
      },
      global: {
        plugins: [i18n],
        stubs: {
          NuxtImg: defineComponent({
            name: 'NuxtImgStub',
            props: ['src'],
            setup(props) {
              return () =>
                h('img', { class: 'nuxt-img-stub', src: props.src as string })
            },
          }),
          ImpactCoefficientBadge: defineComponent({
            name: 'ImpactCoefficientBadgeStub',
            props: ['value'],
            setup(props) {
              return () => h('span', `coefficient:${props.value}`)
            },
          }),
          ProductImpactSubscoreChart: defineComponent({
            name: 'ProductImpactSubscoreChartStub',
            setup() {
              return () => h('div', { class: 'chart-stub' })
            },
          }),
          ProductImpactSubscoreExplanation: defineComponent({
            name: 'ProductImpactSubscoreExplanationStub',
            setup() {
              return () => h('div', { class: 'explanation-stub' })
            },
          }),
          'v-expansion-panels': defineComponent({
            name: 'VExpansionPanelsStub',
            setup(_, { slots }) {
              return () =>
                h('div', { class: 'expansion-panels-stub' }, slots.default?.())
            },
          }),
          'v-expansion-panel': defineComponent({
            name: 'VExpansionPanelStub',
            setup(_, { slots }) {
              return () =>
                h('div', { class: 'expansion-panel-stub' }, slots.default?.())
            },
          }),
          'v-expansion-panel-title': defineComponent({
            name: 'VExpansionPanelTitleStub',
            setup(_, { slots }) {
              return () =>
                h(
                  'div',
                  { class: 'expansion-panel-title-stub' },
                  slots.default?.()
                )
            },
          }),
          'v-expansion-panel-text': defineComponent({
            name: 'VExpansionPanelTextStub',
            setup(_, { slots }) {
              return () =>
                h(
                  'div',
                  { class: 'expansion-panel-text-stub' },
                  slots.default?.()
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

    const image = wrapper.find('img.nuxt-img-stub')
    expect(image.exists()).toBe(true)
    expect(image.attributes('src')).toBe('/images/reparability/6.4.svg')
  })
})
