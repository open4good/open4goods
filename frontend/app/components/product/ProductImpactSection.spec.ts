import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { defineComponent, h } from 'vue'
import { createI18n } from 'vue-i18n'
import ProductImpactSection from './ProductImpactSection.vue'

const i18nMessages = {
  'fr-FR': {
    product: {
      impact: {
        endOfLife: 'Produit plus commercialisé par {brand} !',
        alternatives: {
          title: 'Alternatives',
        },
        explanationTitle: 'Explication',
      },
    },
  },
  'en-US': {
    product: {
      impact: {
        endOfLife: 'Product no longer marketed by {brand}!',
        alternatives: {
          title: 'Alternatives',
        },
        explanationTitle: 'Explanation',
      },
    },
  },
}

describe('ProductImpactSection', () => {
  const mountComponent = async (props: any = {}) => {
    const i18n = createI18n({
      legacy: false,
      locale: 'fr-FR',
      messages: i18nMessages,
    })

    return mountSuspended(ProductImpactSection, {
      props: {
        scores: [],
        showRadar: false,
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
        },
      },
    })
  }

  it('renders end of life alert when onMarketEndDate is in the past', async () => {
    const pastDate = new Date('2020-01-01')
    const wrapper = await mountComponent({ onMarketEndDate: pastDate })
    expect(wrapper.text()).toContain('Produit plus commercialisé par Brand A !')
  })

  it('does not render end of life alert when onMarketEndDate is in the future', async () => {
    const futureDate = new Date('2030-01-01')
    const wrapper = await mountComponent({ onMarketEndDate: futureDate })
    expect(wrapper.text()).not.toContain('Produit plus commercialisé')
  })

  it('does not render end of life alert when onMarketEndDate is null', async () => {
    const wrapper = await mountComponent({ onMarketEndDate: null })
    expect(wrapper.text()).not.toContain('Produit plus commercialisé')
  })
})
