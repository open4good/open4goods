import { mountSuspended } from '@nuxt/test-utils/runtime'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import { createI18n } from 'vue-i18n'
import type { CommercialEvent, ProductDto } from '~~/shared/api-client'
import ProductPriceSection from './ProductPriceSection.vue'

vi.mock('vue-echarts', () => ({
  default: defineComponent({
    name: 'VueECharts',
    props: { option: { type: Object, default: () => ({}) } },
    setup(props, { slots }) {
      return () =>
        h(
          'div',
          {
            class: 'echart-stub',
            'data-option': JSON.stringify(props.option ?? {}),
          },
          slots.default?.()
        )
    },
  }),
}))

const i18nMessages = {
  'fr-FR': {
    product: {
      price: {
        title: 'Prix et évolution',
        subtitle: 'Suivez l’évolution des prix et comparez les offres.',
        newOffers: 'Offres neuves',
        occasionOffers: "Offres d'occasion",
        trend: {
          decrease: 'Baisse de {amount}',
          increase: 'Hausse de {amount}',
          stable: 'Prix stable',
        },
        noOccasionHistory: "Pas d'historique pour les offres d'occasion.",
        offerList: 'Liste des offres',
        metrics: {
          bestOffer: 'Meilleure offre',
          unknownSource: 'Source inconnue',
          lowest: 'Prix le plus bas',
          average: 'Prix moyen',
          highest: 'Prix le plus haut',
          viewOffer: "Ouvrir l'offre chez {source}",
        },
        events: {
          toggleLabel: 'Afficher les offres commerciales',
          detailsTitle: 'Événement commercial',
          clearSelection: 'Effacer la sélection',
          dateLabel: 'Dates :',
          dateRange: '{start} au {end}',
          singleDay: '{date}',
          untitled: 'Événement commercial',
        },
        noHistory:
          "L'historique des prix n'est pas encore disponible pour ce produit.",
        competition: {
          title: 'Niveau de concurrence',
          count: '{count} offres',
          low: 'Concurrence faible !',
          lowDescription:
            'Peu d’offres disponibles, la comparaison est limitée.',
          correct: 'Concurrence correcte !',
          correctDescription: 'Assez d’offres pour comparer en toute sérénité.',
          super: 'Super concurrence !',
          superDescription:
            'Beaucoup d’offres pour décrocher le meilleur prix.',
        },
        headers: {
          source: 'Source',
          offer: 'Offre',
          price: 'Prix',
          condition: 'État',
          updated: 'Mise à jour',
        },
        condition: {
          new: 'Neuf',
          occasion: 'Occasion',
          used: 'Occasion',
          refurbished: 'Reconditionné',
          unknown: 'Inconnu',
        },
      },
    },
  },
  'en-US': {
    product: {
      price: {
        title: 'Prices and trends',
        subtitle: 'Track price evolution and compare offers.',
        newOffers: 'New offers',
        occasionOffers: 'Second-hand offers',
        trend: {
          decrease: 'Price drop of {amount}',
          increase: 'Price increase of {amount}',
          stable: 'Price unchanged',
        },
        noOccasionHistory: 'No history for second-hand offers.',
        offerList: 'Offer list',
        metrics: {
          bestOffer: 'Best offer',
          unknownSource: 'Unknown source',
          lowest: 'Lowest price',
          average: 'Average price',
          highest: 'Highest price',
          viewOffer: 'Open offer from {source}',
        },
        events: {
          toggleLabel: 'Show commercial offers',
          detailsTitle: 'Commercial event',
          clearSelection: 'Clear selection',
          dateLabel: 'Dates:',
          dateRange: '{start} to {end}',
          singleDay: '{date}',
          untitled: 'Commercial event',
        },
        competition: {
          title: 'Competition level',
          count: '{count} offers',
          low: 'Low competition!',
          lowDescription: 'Few offers available, limited comparison.',
          correct: 'Healthy competition!',
          correctDescription: 'Enough offers to compare with confidence.',
          super: 'Super competition!',
          superDescription: 'Plenty of offers to secure the best price.',
        },
        headers: {
          source: 'Source',
          offer: 'Offer',
          price: 'Price',
          condition: 'Condition',
          updated: 'Updated',
        },
        condition: {
          new: 'New',
          occasion: 'Second-hand',
          used: 'Second-hand',
          refurbished: 'Refurbished',
          unknown: 'Unknown',
        },
      },
    },
  },
}

describe('ProductPriceSection', () => {
  const baseOffers: NonNullable<ProductDto['offers']> = {
    offersCount: 3,
    hasOccasions: true,
    bestPrice: {
      datasourceName: 'Merchant B',
      price: 799,
      currency: 'EUR',
      url: 'https://merchant-b.example',
      compensation: 2.5,
      affiliationToken: 'shared-token',
    },
    bestNewOffer: {
      datasourceName: 'Merchant B',
      price: 799,
      currency: 'EUR',
      url: 'https://merchant-b.example',
      compensation: 2.5,
      affiliationToken: 'abc123',
      favicon: 'https://merchant-b.example/icon.png',
    },
    bestOccasionOffer: {
      datasourceName: 'Merchant U',
      price: 649,
      currency: 'EUR',
      url: 'https://merchant-u.example',
      compensation: 1.5,
      condition: 'OCCASION',
      affiliationToken: 'def456',
      favicon: 'https://merchant-u.example/icon.png',
    },
    offersByCondition: {
      NEW: [
        {
          datasourceName: 'Merchant C',
          offerName: 'Offer C',
          price: 899,
          currency: 'EUR',
          condition: 'NEW',
          timeStamp: Date.UTC(2024, 5, 10),
          url: 'https://merchant-c.example',
          favicon: 'https://merchant-c.example/icon.png',
        },
        {
          datasourceName: 'Merchant B',
          offerName: 'Offer B',
          price: 799,
          currency: 'EUR',
          condition: 'NEW',
          timeStamp: Date.UTC(2024, 5, 8),
          url: 'https://merchant-b.example',
          favicon: 'https://merchant-b.example/icon.png',
        },
      ],
      OCCASION: [
        {
          datasourceName: 'Merchant U',
          offerName: 'Offer U',
          price: 649,
          currency: 'EUR',
          condition: 'OCCASION',
          timeStamp: Date.UTC(2024, 5, 6),
          url: 'https://merchant-u.example',
          favicon: 'https://merchant-u.example/icon.png',
        },
      ],
    },
    newHistory: {
      entries: [
        { timestamp: Date.UTC(2024, 3, 1), price: 949 },
        { timestamp: Date.UTC(2024, 4, 1), price: 899 },
        { timestamp: Date.UTC(2024, 5, 1), price: 799 },
      ],
    },
    occasionHistory: {
      entries: [
        { timestamp: Date.UTC(2024, 3, 20), price: 729 },
        { timestamp: Date.UTC(2024, 4, 15), price: 699 },
        { timestamp: Date.UTC(2024, 5, 1), price: 649 },
      ],
    },
    newTrend: {
      trend: 'PRICE_DECREASE',
      variation: -50,
    },
    historyPriceGap: 120,
  }

  const commercialEvents: CommercialEvent[] = [
    {
      label: 'Summer sales',
      startDate: new Date(Date.UTC(2024, 5, 5)),
      endDate: new Date(Date.UTC(2024, 5, 12)),
    },
  ]

  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(Date.UTC(2024, 5, 15)))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const mountComponent = async (
    overrides?: Partial<NonNullable<ProductDto['offers']>>
  ) => {
    const i18n = createI18n({
      legacy: false,
      locale: 'fr-FR',
      fallbackLocale: 'en-US',
      messages: i18nMessages,
    })

    const offers = JSON.parse(JSON.stringify(baseOffers)) as typeof baseOffers
    Object.assign(offers, overrides)

    return mountSuspended(ProductPriceSection, {
      props: {
        offers,
        commercialEvents,
      },
      global: {
        plugins: [[i18n]],
        components: {
          ClientOnly: defineComponent({
            name: 'ClientOnlyStub',
            setup(_, { slots }) {
              return () => slots.default?.()
            },
          }),
          NuxtLink: defineComponent({
            name: 'NuxtLinkStub',
            props: { to: { type: String, required: true } },
            setup(props, { slots }) {
              return () =>
                h(
                  'a',
                  { href: props.to, class: 'nuxt-link-stub' },
                  slots.default?.()
                )
            },
          }),
        },
      },
    })
  }

  it('renders a trend label when prices decrease', async () => {
    const wrapper = await mountComponent()

    const trend = wrapper.find('.product-price__trend')
    expect(trend.exists()).toBe(true)
    expect(trend.text()).toMatch(/Baisse|drop/i)

    await wrapper.unmount()
  })

  it('annotates charts with commercial events', async () => {
    const wrapper = await mountComponent()

    const chart = wrapper.find('.echart-stub')
    const option = JSON.parse(chart.attributes('data-option') ?? '{}')
    const lineSeries = option?.series?.find(
      (series: { type?: string }) => series.type === 'line'
    )
    const eventSeries = option?.series?.find(
      (series: { type?: string }) => series.type === 'custom'
    )
    expect(option?.grid?.length).toBe(2)
    expect(option?.xAxis?.length).toBe(2)
    expect(eventSeries?.data?.[0]?.value?.[3]).toBe('Summer sales')
    expect(lineSeries?.type).toBe('line')
    expect(lineSeries?.showSymbol).toBe(false)
    expect(lineSeries?.smooth).toBe(true)

    await wrapper.unmount()
  })

  it('renders metrics beneath each chart', async () => {
    const wrapper = await mountComponent()

    const metrics = wrapper.findAll('.product-price__metrics')
    expect(metrics).toHaveLength(2)
    expect(metrics[0]?.text()).toMatch(/799/)
    expect(metrics[0]?.text()).toMatch(/Prix le plus bas/)
    expect(wrapper.findAll('.product-price__metrics-stat-icon')).toHaveLength(6)

    await wrapper.unmount()
  })

  it('links to the affiliation redirect when best offers have a token', async () => {
    const wrapper = await mountComponent()

    const links = wrapper.findAll('.product-price__best-offer-card [href]')
    expect(links.length).toBeGreaterThanOrEqual(1)
    const hrefs = links.map(link => link.attributes('href'))
    expect(hrefs).toContain('/contrib/abc123')

    await wrapper.unmount()
  })

  it('displays compact favicon sizes for best offer highlights', async () => {
    const wrapper = await mountComponent()

    const icons = wrapper.findAll('.product-price__best-offer-card img')
    expect(icons.length).toBeGreaterThanOrEqual(1)
    expect(icons[0]?.attributes('src')).toContain('merchant-b.example/icon.png')

    await wrapper.unmount()
  })

  it('omits best offer highlight when no best new offer is available', async () => {
    const wrapper = await mountComponent({ bestNewOffer: undefined })

    expect(wrapper.find('.product-price__best-offer-card').exists()).toBe(false)

    await wrapper.unmount()
  })

  it('hides charts when history has fewer than three points', async () => {
    const wrapper = await mountComponent({
      newHistory: {
        entries: [{ timestamp: Date.UTC(2024, 4, 1), price: 899 }],
      },
      occasionHistory: {
        entries: [
          { timestamp: Date.UTC(2024, 5, 1), price: 649 },
          { timestamp: Date.UTC(2024, 4, 15), price: 699 },
        ],
      },
    })

    expect(wrapper.findAll('.echart-stub')).toHaveLength(0)
    expect(wrapper.find('.product-price__charts-empty-message').exists()).toBe(
      true
    )

    await wrapper.unmount()
  })

  it('does not render a low competition card for < 2 offers', async () => {
    const wrapper = await mountComponent({
      offersByCondition: {
        NEW: [
          {
            datasourceName: 'Merchant B',
            price: 799,
            currency: 'EUR',
            condition: 'NEW',
            url: 'https://merchant-b.example',
          },
        ],
      },
      offersCount: 1,
    })

    const card = wrapper.find('.product-price__competition-card')
    expect(card.exists()).toBe(false)
    await wrapper.unmount()
  })

  it('does not render a competition card for 2 offers', async () => {
    const wrapper = await mountComponent({
      offersByCondition: {
        NEW: [
          {
            datasourceName: 'Merchant A',
            price: 100,
            currency: 'EUR',
            condition: 'NEW',
          },
          {
            datasourceName: 'Merchant B',
            price: 200,
            currency: 'EUR',
            condition: 'NEW',
          },
        ],
      },
      offersCount: 2,
    })

    const card = wrapper.find('.product-price__competition-card')
    expect(card.exists()).toBe(false)
    await wrapper.unmount()
  })

  it('renders a correct competition card for 3-4 offers', async () => {
    const wrapper = await mountComponent({
      offersByCondition: {
        NEW: [
          {
            datasourceName: 'Merchant A',
            price: 100,
            currency: 'EUR',
            condition: 'NEW',
          },
          {
            datasourceName: 'Merchant B',
            price: 200,
            currency: 'EUR',
            condition: 'NEW',
          },
          {
            datasourceName: 'Merchant C',
            price: 300,
            currency: 'EUR',
            condition: 'NEW',
          },
        ],
      },
      offersCount: 3,
    })

    const card = wrapper.find('.product-price__competition-card')
    expect(card.exists()).toBe(true)
    expect(card.text()).toContain('Concurrence correcte !')
    expect(card.text()).toContain(
      'Assez d’offres pour comparer en toute sérénité.'
    )

    await wrapper.unmount()
  })

  it('renders a super competition card for > 4 offers', async () => {
    const wrapper = await mountComponent({
      offersByCondition: {
        NEW: Array(5).fill({
          datasourceName: 'Merchant',
          price: 100,
          currency: 'EUR',
          condition: 'NEW',
        }),
      },
      offersCount: 5,
    })

    const card = wrapper.find('.product-price__competition-card')
    expect(card.exists()).toBe(true)
    expect(card.text()).toContain('Super concurrence !')
    expect(card.text()).toContain(
      'Beaucoup d’offres pour décrocher le meilleur prix.'
    )

    await wrapper.unmount()
  })
})
