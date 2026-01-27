import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import ProductImpactSection from './ProductImpactSection.vue'

const i18nMessages = {
  'fr-FR': {
    product: {
      impact: {
        endOfLifeTitle: 'Produit plus commercialisé',
        endOfLifeDescription:
          "Le produit n'est plus officiellement commercialisé par {brand}. La durée de support légal a démarré le {onMarketEndDate}.",
        endOfLifeDescriptionFallback:
          "Le produit n'est plus officiellement commercialisé par {brand}.",
        endOfLifeSupportEnd: 'Fin du support légal',
        endOfLifeSupportDuration: 'Durée de support légal',
        endOfLifeSupportRemaining: 'Support légal restant',
        endOfLifeSupportExpired: 'Support légal terminé',
        alternatives: {
          title: 'Alternatives',
        },
        explanationTitle: 'Explication',
      },
    },
    common: {
      count: {
        years: '1 an | {count} ans',
        months: '1 mois | {count} mois',
      },
    },
  },
  'en-US': {
    product: {
      impact: {
        endOfLifeTitle: 'Product no longer marketed',
        endOfLifeDescription:
          'This product is no longer officially marketed by {brand}. Legal support started on {onMarketEndDate}.',
        endOfLifeDescriptionFallback:
          'This product is no longer officially marketed by {brand}.',
        endOfLifeSupportEnd: 'Legal support ends',
        endOfLifeSupportDuration: 'Legal support duration',
        endOfLifeSupportRemaining: 'Legal support remaining',
        endOfLifeSupportExpired: 'Legal support ended',
        alternatives: {
          title: 'Alternatives',
        },
        explanationTitle: 'Explanation',
      },
    },
    common: {
      count: {
        years: '1 year | {count} years',
        months: '1 month | {count} months',
      },
    },
  },
}

describe('ProductImpactSection', () => {
  const mountComponent = async (props: Record<string, unknown> = {}) => {
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
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
    try {
      const pastDate = new Date('2020-01-01T00:00:00Z')
      const wrapper = await mountComponent({
        onMarketEndDate: pastDate,
        eprelData: {
          eprelDatas: {
            categorySpecificAttributes: {
              minGuaranteedSupportYears: 10,
            },
          },
        },
      })

      expect(wrapper.find('[data-testid="end-of-life-card"]').exists()).toBe(
        true
      )
      expect(wrapper.text()).toContain('Produit plus commercialisé')
      expect(wrapper.text()).toContain('Durée de support légal')
      expect(wrapper.text()).toContain('10 ans')
      expect(wrapper.text()).toContain('Support légal restant')
      expect(wrapper.text()).toContain('4 ans')
    } finally {
      vi.useRealTimers()
    }
  })

  it('does not render end of life alert when onMarketEndDate is in the future', async () => {
    const futureDate = new Date('2030-01-01')
    const wrapper = await mountComponent({ onMarketEndDate: futureDate })
    expect(wrapper.find('[data-testid="end-of-life-card"]').exists()).toBe(
      false
    )
  })

  it('does not render end of life alert when onMarketEndDate is null', async () => {
    const wrapper = await mountComponent({ onMarketEndDate: null })
    expect(wrapper.find('[data-testid="end-of-life-card"]').exists()).toBe(
      false
    )
  })
})
