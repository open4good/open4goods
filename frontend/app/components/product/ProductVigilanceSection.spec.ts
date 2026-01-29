import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import ProductVigilanceSection from './ProductVigilanceSection.vue'

const i18nMessages = {
  'fr-FR': {
    product: {
      impact: {
        endOfLifeTitle: 'Produit plus commercialisé',
        endOfLifeDescription:
          "Le produit n'est plus officiellement commercialisé par {brand}. La durée de support légal a démarré le {onMarketEndDate}.",
        endOfLifeDescriptionFallback:
          "Le produit n'est plus officiellement commercialisé par {brand}.",
      },
      vigilance: {
        title: 'Points de vigilance',
        subtitle: 'Informations importantes sur la durabilité et la qualité.',
      },
      price: {
        competition: {
          title: 'Niveau de concurrence',
          lowDescription:
            'Peu d’offres disponibles, la comparaison est limitée.',
          count: '{count} offres',
        },
      },
    },
  },
}

describe('ProductVigilanceSection', () => {
  const mountComponent = async (props: Record<string, unknown> = {}) => {
    const i18n = createI18n({
      legacy: false,
      locale: 'fr-FR',
      messages: i18nMessages,
    })

    return mountSuspended(ProductVigilanceSection, {
      props: {
        product: {
          identity: { brand: 'Brand A' },
          ...((props.product as object) || {}),
        },
        ...props,
      },
      global: {
        plugins: [[i18n]],
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
      })

      expect(wrapper.find('.product-vigilance__card--eol').exists()).toBe(true)
      expect(wrapper.text()).toContain('Produit plus commercialisé')
      expect(wrapper.text()).toContain('Brand A')
    } finally {
      vi.useRealTimers()
    }
  })

  it('does not render end of life alert when onMarketEndDate is in the future', async () => {
    const futureDate = new Date('2030-01-01')
    const wrapper = await mountComponent({ onMarketEndDate: futureDate })
    expect(wrapper.find('.product-vigilance__card--eol').exists()).toBe(false)
  })

  it('renders low competition vigilance when there are 2 offers', async () => {
    const wrapper = await mountComponent({
      product: {
        offers: {
          offersByCondition: {
            NEW: [{ price: 100 }, { price: 200 }],
          },
        },
      },
    })
    expect(wrapper.find('.product-vigilance__card--competition').exists()).toBe(
      true
    )
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('offres')
  })

  it('renders low competition vigilance when there is 1 offer', async () => {
    const wrapper = await mountComponent({
      product: {
        offers: {
          offersByCondition: {
            NEW: [{ price: 100 }],
          },
        },
      },
    })
    expect(wrapper.find('.product-vigilance__card--competition').exists()).toBe(
      true
    )
    console.log(wrapper.html())
    expect(wrapper.text()).toContain('1')
    expect(wrapper.text()).toContain('offres')
  })

  it('does not render low competition vigilance when there are 3 offers', async () => {
    const wrapper = await mountComponent({
      product: {
        offers: {
          offersByCondition: {
            NEW: [{ price: 100 }, { price: 200 }, { price: 300 }],
          },
        },
      },
    })
    expect(wrapper.find('.product-vigilance__card--competition').exists()).toBe(
      false
    )
  })

  it('does not render low competition vigilance when there are 0 offers', async () => {
    const wrapper = await mountComponent({
      product: {
        offers: {
          offersByCondition: {
            NEW: [],
          },
        },
      },
    })
    expect(wrapper.find('.product-vigilance__card--competition').exists()).toBe(
      false
    )
  })
})
