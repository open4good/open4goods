import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import ProductVigilanceSection from './ProductVigilanceSection.vue'

// Mock the child component to avoid import issues and simplify testing
const ProductAttributeSourcingLabel = {
  template: '<div><slot /></div>',
  props: ['sourcing', 'enableTooltip'],
}

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
        conflicts: {
          title: 'Conflits',
          description: 'Description des conflits',
          showMore: 'Voir plus',
          showLess: 'Voir moins',
        },
        quality: {
          title: 'Qualité des données',
          description: 'Score: {score}, Moyenne: {avg}',
        },
        obsolescence: {
          title: 'Obsolescence',
        },
      },
      price: {
        competition: {
          title: 'Niveau de concurrence',
          lowDescription:
            'Peu d’offres disponibles, la comparaison est limitée.',
          count: '{count} offres',
          cta: 'Voir les {count} offres',
        },
      },
    },
  },
}

describe('ProductVigilanceSection', () => {
  const vuetify = createVuetify({
    components,
    directives,
  })

  // Create a fresh i18n instance for each test to avoid pollution?
  // Actually createI18n inside mountComponent is safer.

  const mountComponent = async (props: Record<string, unknown> = {}) => {
    const i18n = createI18n({
      legacy: false,
      locale: 'fr-FR',
      messages: i18nMessages,
    })

    return mount(ProductVigilanceSection, {
      props: {
        product: {
          identity: { brand: 'Brand A' },
          ...((props.product as object) || {}),
        },
        ...props,
      },
      global: {
        plugins: [vuetify, i18n],
        stubs: {
          ProductAttributeSourcingLabel,
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

  it('renders HTML in obsolescence warning', async () => {
    const htmlWarning =
      '<strong>Warning!</strong> This is <a href="#">AI generated</a>.'
    const wrapper = await mountComponent({
      product: {
        aiReview: {
          review: {
            obsolescenceWarning: htmlWarning,
          },
        },
      },
    })

    const description = wrapper.find(
      '.product-vigilance__card--obsolescence .product-vigilance__card-description'
    )
    expect(description.html()).toContain('<strong>Warning!</strong>')
    expect(description.html()).toContain('<a href="#">AI generated</a>')
  })

  it('renders HTML in data quality description', async () => {
    const i18n = createI18n({
      legacy: false,
      locale: 'fr-FR',
      messages: {
        'fr-FR': {
          product: {
            vigilance: {
              quality: {
                description: 'Score: <strong>{score}</strong>/20',
              },
            },
          },
        },
      },
    })

    const wrapper = mount(ProductVigilanceSection, {
      props: {
        product: {
          scores: {
            scores: {
              DATA_QUALITY: {
                value: 2, // 2 * 4 = 8.0
                relativ: { avg: 4 }, // 4 * 4 = 16.0
              },
            },
          },
        },
      },
      global: {
        plugins: [vuetify, i18n],
        stubs: {
          ProductAttributeSourcingLabel,
        },
      },
    })

    const description = wrapper.find(
      '.product-vigilance__card--quality .product-vigilance__card-description'
    )
    expect(description.html()).toContain('<strong>8.0</strong>')
  })
})
