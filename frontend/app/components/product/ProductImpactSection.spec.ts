import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import ProductImpactSection from './ProductImpactSection.vue'

describe('ProductImpactSection', () => {
  const mountComponent = async (props: Record<string, unknown> = {}) => {
    const i18n = createI18n({
      legacy: false,
      locale: 'fr-FR',
      messages: { 'fr-FR': { product: { impact: { title: 'Impact' } } } },
    })

    return mountSuspended(ProductImpactSection, {
      props: {
        scores: [],
        productName: 'Test Product',
        productBrand: 'Brand A',
        ...props,
      },
      global: {
        plugins: [[i18n]],
        stubs: {
          ProductImpactEcoScoreCard: true,
          ImpactRadar: true,
          ProductAlternatives: true,
          ImpactScore: true,
          EprelDetailsTable: true,
        },
      },
    })
  }

  it('renders correctly', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.exists()).toBe(true)
  })
})
