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
        h('div', { class: 'echart-stub', 'data-option': JSON.stringify(props.option ?? {}) }, slots.default?.())
    },
  }),
}))

const TableStub = defineComponent({
  name: 'VTableStub',
  setup(_, { slots }) {
    return () => h('table', { class: 'v-table-stub' }, slots.default?.())
  },
})

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
        },
        noHistory: "L'historique des prix n'est pas encore disponible pour ce produit.",
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
        },
        noHistory: 'Price history is not yet available for this product.',
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
    },
    bestNewOffer: {
      datasourceName: 'Merchant B',
      price: 799,
      currency: 'EUR',
      url: 'https://merchant-b.example',
      compensation: 2.5,
    },
    bestOccasionOffer: {
      datasourceName: 'Merchant U',
      price: 649,
      currency: 'EUR',
      url: 'https://merchant-u.example',
      compensation: 1.5,
      condition: 'OCCASION',
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

  const mountComponent = async (overrides?: Partial<NonNullable<ProductDto['offers']>>) => {
    const i18n = createI18n({ legacy: false, locale: 'fr-FR', fallbackLocale: 'en-US', messages: i18nMessages })

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
          'v-table': TableStub,
        },
      },
    })
  }

  it('sorts offers by ascending price in the table', async () => {
    const wrapper = await mountComponent()

    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(3)
    const firstCells = rows[0]?.findAll('td') ?? []
    const secondCells = rows[1]?.findAll('td') ?? []
    expect(firstCells[1]?.text()).toContain('Offer U')
    expect(secondCells[1]?.text()).toContain('Offer B')

    await wrapper.unmount()
  })

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
    const markAreaLabel = option?.series?.[0]?.markArea?.data?.[0]?.[1]?.label?.formatter
    expect(markAreaLabel).toBe('Summer sales')
    expect(option?.series?.[0]?.type).toBe('bar')

    await wrapper.unmount()
  })

  it('renders metrics beneath each chart', async () => {
    const wrapper = await mountComponent()

    const metrics = wrapper.findAll('.product-price__metrics')
    expect(metrics).toHaveLength(2)
    expect(metrics[0]?.text()).toContain('Meilleure offre')
    expect(metrics[0]?.text()).toMatch(/799/)
    expect(metrics[0]?.text()).toMatch(/Prix le plus bas/)

    await wrapper.unmount()
  })

  it('hides charts when history has fewer than three points', async () => {
    const wrapper = await mountComponent({
      newHistory: { entries: [{ timestamp: Date.UTC(2024, 4, 1), price: 899 }] },
      occasionHistory: { entries: [{ timestamp: Date.UTC(2024, 5, 1), price: 649 }, { timestamp: Date.UTC(2024, 4, 15), price: 699 }] },
    })

    expect(wrapper.findAll('.echart-stub')).toHaveLength(0)
    expect(wrapper.find('.product-price__charts-empty-message').exists()).toBe(true)

    await wrapper.unmount()
  })
})
