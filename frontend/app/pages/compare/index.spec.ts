import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, reactive, ref } from 'vue'
import { flushPromises } from '@vue/test-utils'

type CompareProductEntryLike = {
  gtin: string
  verticalId: string | null
  product: Record<string, unknown>
  title: string
  brand: string | null
  model: string | null
  coverImage: string | null
  impactScore: number | null
  review: { description: string | null; pros: string[]; cons: string[] }
  country: { name: string; flag?: string } | null
}

const loadProductsMock = vi.fn<[string[]], Promise<CompareProductEntryLike[]>>()
const loadVerticalMock = vi.fn<[string | null], Promise<Record<string, unknown> | null>>()
const hasMixedVerticalsMock = vi.fn<[CompareProductEntryLike[]], boolean>()

vi.mock('~/services/compare/CompareService', () => ({
  createCompareService: () => ({
    loadProducts: loadProductsMock,
    loadVertical: loadVerticalMock,
    hasMixedVerticals: hasMixedVerticalsMock,
  }),
}))

const compareStore = {
  clear: vi.fn(),
  addProduct: vi.fn(),
  removeById: vi.fn(),
}

vi.mock('~/stores/useProductCompareStore', () => ({
  useProductCompareStore: () => compareStore,
}))

vi.mock('~/utils/_compare-url', () => ({
  parseCompareHash: vi.fn(() => ['1234567890123']),
  buildCompareHash: vi.fn(() => '#1234567890123'),
}))

vi.mock('~/utils/_product-pricing', () => ({
  formatBestPrice: () => null,
  formatOffersCount: () => null,
}))

vi.mock('~/utils/_product-attributes', () => ({
  formatAttributeValue: () => null,
  resolveAttributeRawValueByKey: () => null,
}))

vi.mock('~/components/shared/ui/ImpactScore.vue', () => ({
  default: defineComponent({
    name: 'ImpactScoreStub',
    props: { score: { type: Number, default: 0 } },
    setup(props) {
      return () => h('div', { class: 'impact-score-stub' }, `score:${props.score}`)
    },
  }),
}))

const localeRef = ref('fr-FR')

const messages: Record<string, string> = {
  'compare.title': 'Comparateur de produits',
  'compare.subtitle':
    'Analysez les fiches techniques, les prix et les indicateurs écologiques des produits sélectionnés.',
  'compare.hero.title': 'Comparateur de {verticalTitle}',
  'compare.hero.subtitle':
    'Analysez les fiches techniques, les prix et les indicateurs écologiques des {verticalTitle} sélectionnés.',
  'compare.hero.backToCategory': 'Retour aux {verticalTitle}',
  'compare.hero.backAria': 'Retourner à la catégorie {verticalTitle}',
  'compare.hero.backFallback': 'Retour à la catégorie',
  'compare.hero.backAriaFallback': 'Revenir à la catégorie précédente',
  'compare.a11y.featureColumn': 'Critères',
  'compare.a11y.viewProduct': 'Voir {name}',
  'compare.a11y.bestValue': 'Meilleure valeur',
  'compare.textual.description': 'Description',
  'compare.textual.pros': 'Atouts',
  'compare.textual.cons': 'Limites',
  'compare.textual.empty': '—',
  'compare.pricing.newPrice': 'Prix neuf',
  'compare.pricing.occasionPrice': 'Prix occasion',
  'compare.pricing.offersCount': 'Nombre d’offres',
  'compare.ecological.ecoscore': 'Écoscore',
  'compare.ecological.brandSustainability': 'Durabilité marque',
  'compare.ecological.dataQuality': 'Qualité des données',
  'compare.sections.pricing': 'Tarifs',
  'compare.sections.ecological': 'Impact écologique',
  'compare.sections.technical': 'Fiche technique',
  'compare.sections.technicalGroupFallback': 'Autres',
  'compare.alerts.verticalMismatch': 'Alerte',
  'compare.actions.remove': 'Retirer {name}',
  'compare.actions.removeShort': 'Retirer',
  'compare.states.loading': 'Chargement',
  'compare.empty.title': 'Ajoutez des produits',
  'compare.empty.description': 'Utilisez le bouton comparer.',
  'compare.errors.loadFailed': 'Échec de chargement',
  'category.products.compare.itemsCount': '{count} produits',
  'components.impactScore.tooltip': '{value} sur {max}',
}

const translate = (key: string, params: Record<string, unknown> = {}) => {
  const template = messages[key] ?? key
  return template.replace(/\{(\w+)\}/g, (_, match) => String(params[match] ?? ''))
}

mockNuxtImport('useI18n', () => () => ({
  t: (key: string, params: Record<string, unknown> = {}) => translate(key, params),
  n: (value: number | bigint, _options?: Intl.NumberFormatOptions) => String(value),
  locale: localeRef,
}))

mockNuxtImport('useId', () => () => 'compare-hero')

const route = reactive({ hash: '#compare=1234567890123' })
const routerReplace = vi.fn()

mockNuxtImport('useRoute', () => () => route)
mockNuxtImport('useRouter', () => () => ({ replace: routerReplace }))

vi.mock('~/composables/usePluralizedTranslation', () => ({
  usePluralizedTranslation: () => ({
    translatePlural: (_key: string, count: number) => `${count} produits`,
  }),
}))

mockNuxtImport('~~/shared/utils/localized-routes', () => ({
  resolveLocalizedRoutePath: () => '/compare',
}))

const simpleStub = (tag: string) =>
  defineComponent({
    name: `${tag}-stub`,
    setup(_props, { slots, attrs }) {
      return () => h(tag, attrs, slots.default?.())
    },
  })

const mountPage = async () => {
  const component = (await import('./index.vue')).default

  return mountSuspended(component, {
    global: {
      stubs: {
        VContainer: simpleStub('div'),
        VIcon: defineComponent({
          name: 'VIconStub',
          props: { icon: { type: String, default: '' }, size: { type: [Number, String], default: 24 } },
          setup(props) {
            return () => h('span', { class: 'v-icon-stub', 'data-icon': props.icon }, props.icon)
          },
        }),
        VAlert: simpleStub('div'),
        VProgressLinear: simpleStub('div'),
        VBtn: defineComponent({
          name: 'VBtnStub',
          props: { type: { type: String, default: 'button' } },
          setup(props, { slots, attrs }) {
            return () => h('button', { ...attrs, type: props.type }, slots.default?.())
          },
        }),
        VTooltip: defineComponent({
          name: 'VTooltipStub',
          props: { text: { type: String, default: '' }, location: { type: String, default: '' } },
          setup(_props, { slots }) {
            const activator = slots.activator?.({ props: {} })
            const content = slots.default?.()
            return () => h('div', { class: 'v-tooltip-stub' }, [activator, content])
          },
        }),
        NuxtImg: defineComponent({
          name: 'NuxtImgStub',
          props: { alt: { type: String, default: '' }, src: { type: String, default: '' } },
          setup(props) {
            return () => h('img', { alt: props.alt, src: props.src })
          },
        }),
      },
    },
  })
}

const createEntry = (overrides: Partial<CompareProductEntryLike> = {}): CompareProductEntryLike => ({
  gtin: '1234567890123',
  verticalId: 'vertical-1',
  product: {
    identity: { brand: 'Brand', model: 'Model' },
    base: {},
    offers: {},
    scores: {},
    attributes: {},
  },
  title: 'Produit 1',
  brand: 'Brand',
  model: 'Model',
  coverImage: null,
  impactScore: 4,
  review: { description: null, pros: [], cons: [] },
  country: null,
  ...overrides,
})

describe('Compare page hero', () => {
  beforeEach(() => {
    loadProductsMock.mockReset()
    loadVerticalMock.mockReset()
    hasMixedVerticalsMock.mockReset()
    compareStore.clear.mockReset()
    compareStore.addProduct.mockReset()
    compareStore.removeById.mockReset()
    routerReplace.mockReset()
    route.hash = '#compare=1234567890123'
    localeRef.value = 'fr-FR'
    hasMixedVerticalsMock.mockReturnValue(false)
  })

  it('renders the hero with the vertical title in lowercase when available', async () => {
    loadProductsMock.mockResolvedValue([createEntry({ verticalId: 'vertical-1' })])
    loadVerticalMock.mockResolvedValue({
      verticalHomeTitle: 'Lave-vaisselle',
      verticalHomeUrl: 'electromenager/lave-vaisselle',
    })

    const wrapper = await mountPage()
    await flushPromises()
    await flushPromises()

    expect(wrapper.get('.compare-page__title').text()).toBe('Comparateur de lave-vaisselle')
    expect(wrapper.get('.compare-page__subtitle').text()).toBe(
      'Analysez les fiches techniques, les prix et les indicateurs écologiques des lave-vaisselle sélectionnés.',
    )

    const backLink = wrapper.get('.compare-page__hero-back')
    expect(backLink.text()).toContain('Retour aux lave-vaisselle')
    expect(backLink.attributes('href')).toBe('/electromenager/lave-vaisselle')
  })

  it('falls back to generic hero copy when the vertical configuration is unavailable', async () => {
    loadProductsMock.mockResolvedValue([createEntry({ verticalId: null })])
    loadVerticalMock.mockResolvedValue(null)

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('.compare-page__title').text()).toBe('Comparateur de produits')
    expect(wrapper.get('.compare-page__subtitle').text()).toBe(
      'Analysez les fiches techniques, les prix et les indicateurs écologiques des produits sélectionnés.',
    )
    expect(wrapper.find('.compare-page__hero-back').exists()).toBe(false)
  })
})
