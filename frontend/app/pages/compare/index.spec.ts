import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { reactive } from 'vue'
import { createI18n } from 'vue-i18n'
import { createVuetify } from 'vuetify'
import type { CompareProductEntry } from '~/services/compare/CompareService'
import type {
  AttributeConfigDto,
  ProductDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'

vi.mock('~/composables/usePluralizedTranslation', () => ({
  usePluralizedTranslation: () => ({
    translatePlural: (_key: string, count: number) => (count === 1 ? '1 offer' : `${count} offers`),
  }),
}))

const routerPush = vi.fn(async () => {})
const routerReplace = vi.fn(async () => {})

const route = reactive({
  path: '/compare',
  hash: '#123Vs456',
})

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
    replace: routerReplace,
  }),
  useRoute: () => route,
}))

vi.mock('~~/shared/utils/localized-routes', () => ({
  resolveLocalizedRoutePath: () => '/compare',
}))

const entries: CompareProductEntry[] = [
  {
    gtin: '123',
    product: {
      gtin: 123,
      base: {
        vertical: 'electronics',
        bestName: 'Product A',
        gtinInfo: { countryName: 'France', countryFlagUrl: '/flags/fr.svg' },
      },
      identity: { brand: 'Brand A', model: 'Model A', bestName: 'Product A' },
      resources: { coverImagePath: '/images/a.jpg' },
      offers: {
        bestNewOffer: { price: 100, currency: 'EUR' },
        bestOccasionOffer: { price: 80, currency: 'EUR' },
        offersCount: 5,
      },
      scores: {
        ecoscore: { percent: 60 },
        scores: {
          BRAND_SUSTAINABILITY: { percent: 70 },
          DATA_QUALITY: { percent: 40 },
        },
      },
      attributes: {
        indexedAttributes: {
          POWER: { name: 'Power', value: '500', numericValue: 500 },
          ECO: { name: 'Eco attr', value: '40', numericValue: 40 },
        },
        classifiedAttributes: [
          {
            name: 'Dimensions',
            attributes: [{ name: 'Height', value: '10 cm' }],
          },
        ],
      },
      aiReview: {
        review: {
          description: '<p>A strong product.</p>',
          pros: ['<strong>Efficient</strong>'],
          cons: ['<em>Heavy</em>'],
        },
      },
    } as ProductDto,
    verticalId: 'electronics',
    title: 'Product A',
    brand: 'Brand A',
    model: 'Model A',
    coverImage: '/images/a.jpg',
    impactScore: 3,
    review: {
      description: 'A strong product.',
      pros: ['<strong>Efficient</strong>'],
      cons: ['<em>Heavy</em>'],
    },
    country: { name: 'France', flag: '/flags/fr.svg' },
  },
  {
    gtin: '456',
    product: {
      gtin: 456,
      base: {
        vertical: 'electronics',
        bestName: 'Product B',
        gtinInfo: { countryName: 'Germany', countryFlagUrl: '/flags/de.svg' },
      },
      identity: { brand: 'Brand B', model: 'Model B', bestName: 'Product B' },
      resources: { coverImagePath: '/images/b.jpg' },
      offers: {
        bestNewOffer: { price: 120, currency: 'EUR' },
        bestOccasionOffer: { price: 90, currency: 'EUR' },
        offersCount: 3,
      },
      scores: {
        ecoscore: { percent: 75 },
        scores: {
          BRAND_SUSTAINABILITY: { percent: 65 },
          DATA_QUALITY: { percent: 55 },
        },
      },
      attributes: {
        indexedAttributes: {
          POWER: { name: 'Power', value: '450', numericValue: 450 },
          ECO: { name: 'Eco attr', value: '35', numericValue: 35 },
        },
        classifiedAttributes: [
          {
            name: 'Dimensions',
            attributes: [{ name: 'Height', value: '11 cm' }],
          },
        ],
      },
      aiReview: {
        review: {
          description: '<p>An efficient model.</p>',
          pros: ['<strong>Silent</strong>'],
          cons: ['<em>Bulky</em>'],
        },
      },
    } as ProductDto,
    verticalId: 'electronics',
    title: 'Product B',
    brand: 'Brand B',
    model: 'Model B',
    coverImage: '/images/b.jpg',
    impactScore: 4,
    review: {
      description: 'An efficient model.',
      pros: ['<strong>Silent</strong>'],
      cons: ['<em>Bulky</em>'],
    },
    country: { name: 'Germany', flag: '/flags/de.svg' },
  },
]

const powerConfig: AttributeConfigDto = {
  key: 'POWER',
  name: 'Power',
  icon: 'mdi-flash',
  unit: 'W',
  betterIs: 'GREATER',
}

const ecoConfig: AttributeConfigDto = {
  key: 'ECO',
  name: 'Eco attribute',
  icon: 'mdi-leaf',
  asScore: true,
  betterIs: 'LOWER',
}

const verticalConfig: VerticalConfigFullDto = {
  attributesConfig: {
    configs: [powerConfig, ecoConfig],
  },
  popularAttributes: [powerConfig],
} as VerticalConfigFullDto

vi.mock('~/services/compare/CompareService', async (importOriginal) => {
  const actual = (await importOriginal()) as typeof import('~/services/compare/CompareService')
  return {
    ...actual,
    createCompareService: () => ({
      loadProducts: vi.fn().mockResolvedValue(entries),
      loadVertical: vi.fn().mockResolvedValue(verticalConfig),
      hasMixedVerticals: vi.fn().mockReturnValue(false),
    }),
  }
})

const mountPage = async () => {
  const module = await import('./index.vue')
  const ComparePage = module.default
  const pinia = createPinia()
  setActivePinia(pinia)

  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        compare: {
          title: 'Product comparison',
          subtitle: 'subtitle',
          alerts: { verticalMismatch: 'Mismatch' },
          states: { loading: 'Loading…' },
          empty: { title: 'Empty', description: 'desc' },
          sections: {
            overview: 'Overview',
            pricing: 'Pricing',
            ecological: 'Ecological',
            technical: 'Technical',
            technicalGroupFallback: 'Other specs',
          },
          a11y: {
            featureColumn: 'Feature column',
            viewProduct: 'View {name}',
            bestValue: 'Best value',
          },
          actions: { remove: 'Remove {name}', removeShort: 'Remove' },
          textual: { description: 'Description', pros: 'Pros', cons: 'Cons', empty: 'N/A' },
          pricing: { newPrice: 'New price', occasionPrice: 'Second-hand price', offersCount: 'Offer count' },
          ecological: {
            ecoscore: 'Ecoscore',
            brandSustainability: 'Brand sustainability',
            dataQuality: 'Data quality',
          },
          errors: { loadFailed: 'Error' },
        },
      },
    },
  })

  const vuetify = createVuetify()

  return mount(ComparePage, {
    global: {
      plugins: [pinia, i18n, vuetify],
      stubs: {
        VAlert: { template: '<div class="v-alert"><slot /></div>' },
        VProgressLinear: { template: '<div class="v-progress" />' },
        VIcon: { template: '<span class="v-icon"><slot /></span>' },
        VBtn: { template: '<button><slot /></button>' },
        VTooltip: { template: '<div><slot name="activator" :props="{}"></slot><slot /></div>' },
        NuxtImg: { template: '<img />' },
        ImpactScore: { template: '<div class="impact-score" />' },
      },
    },
  })
}

describe('Compare page', () => {
  beforeEach(() => {
    routerPush.mockReset()
    routerReplace.mockReset()
    routerPush.mockResolvedValue(undefined)
    routerReplace.mockResolvedValue(undefined)
  })

  it('renders fetched products and textual data', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    const productHeaders = wrapper.findAll('.compare-grid__product')
    expect(productHeaders).toHaveLength(2)
    expect(wrapper.text()).toContain('Model A')
    expect(wrapper.text()).toContain('Model B')
    expect(wrapper.text()).toContain('A strong product.')
    expect(wrapper.findAll('.compare-grid__list-item')).toHaveLength(4)
  })

  it('updates the hash when removing a product', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    const removeButton = wrapper.get('.compare-grid__product-remove')
    await removeButton.trigger('click')
    expect(routerReplace).toHaveBeenCalled()
  })
})
