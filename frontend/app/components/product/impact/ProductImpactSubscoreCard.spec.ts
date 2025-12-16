import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import ProductImpactSubscoreCard from './ProductImpactSubscoreCard.vue'
import type { ScoreView } from './impact-types'

vi.mock('./ProductImpactSubscoreGenericCard.vue', () => ({
  default: defineComponent({
    name: 'GenericCardStub',
    setup(_, { attrs }) {
      return () =>
        h(
          'div',
          { class: 'generic-card-stub' },
          attrs['data-testid'] ?? 'generic-card'
        )
    },
  }),
}))

vi.mock('./subscores/ProductImpactSubscoreDataQualityCard.vue', () => ({
  default: defineComponent({
    name: 'DataQualityCardStub',
    setup() {
      return () => h('div', { class: 'data-quality-stub' }, 'data-quality')
    },
  }),
}))

const baseScore: ScoreView = {
  id: 'UNKNOWN',
  label: 'Label',
  description: null,
  relativeValue: 0,
  absoluteValue: null,
  percent: null,
  ranking: null,
  letter: null,
  on20: null,
  distribution: [],
  energyLetter: null,
  metadatas: null,
}

describe('ProductImpactSubscoreCard', () => {
  it('falls back to the generic card for unknown ids', () => {
    const wrapper = mount(ProductImpactSubscoreCard, {
      props: {
        score: baseScore,
        productName: 'Demo',
        productBrand: 'EcoCorp',
        productModel: 'X1',
        productImage: '/cover.png',
        verticalTitle: 'televisions',
      },
    })

    expect(wrapper.find('.generic-card-stub').exists()).toBe(true)
  })

  it('renders the specialized card when available', () => {
    const wrapper = mount(ProductImpactSubscoreCard, {
      props: {
        score: { ...baseScore, id: 'DATA_QUALITY' },
        productName: 'Demo',
        productBrand: 'EcoCorp',
        productModel: 'X1',
        productImage: '/cover.png',
        verticalTitle: 'televisions',
      },
    })

    expect(wrapper.find('.data-quality-stub').exists()).toBe(true)
  })
})
