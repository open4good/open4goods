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
const loadVerticalMock = vi.fn<
  [string | null],
  Promise<Record<string, unknown> | null>
>()
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

vi.mock('~/components/shared/ui/ImpactScore.vue', async () => {
  const { defineComponent: dc, h: hFn } = await import('vue')
  return {
    default: dc({
      name: 'ImpactScoreStub',
      props: { score: { type: Number, default: 0 } },
      setup(props) {
        return () =>
          hFn('div', { class: 'impact-score-stub' }, `score:${props.score}`)
      },
    }),
  }
})

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
  'compare.sections.explicitTitle': 'Comparaison de {count} produits sélectionnés',
  'compare.alerts.verticalMismatch': 'Alerte',
  'compare.actions.remove': 'Retirer {name}',
  'compare.actions.removeShort': 'Retirer',
  'compare.states.loading': 'Chargement',
  'compare.empty.title': 'Ajoutez des produits',
  'compare.empty.description': 'Utilisez le bouton comparer.',
  'compare.errors.loadFailed': 'Échec de chargement',
  'compare.errors.invalidNotEnoughProducts': 'La comparaison nécessite au moins deux produits différents.',
  'compare.errors.incomplete': '{count} produit est indisponible. Retirez-le ou modifiez votre sélection.',
  'compare.provenance.sourceLabel': 'Source',
  'compare.provenance.updatedLabel': 'Dernière mise à jour',
  'compare.provenance.unknown': 'Source non disponible',
  'category.products.compare.itemsCount': '{count} produits',
  'components.impactScore.tooltip': '{value} sur {max}',
}

const translate = (key: string, params: Record<string, unknown> = {}) => {
  const template = messages[key] ?? key
  return template.replace(/\{(\w+)\}/g, (_, match) =>
    String(params[match] ?? '')
  )
}

mockNuxtImport('useI18n', () => () => ({
  t: (key: string, params: Record<string, unknown> = {}) =>
    translate(key, params),
  n: (value: number | bigint, options?: Intl.NumberFormatOptions) => {
    const numericValue = typeof value === 'bigint' ? Number(value) : value
    return new Intl.NumberFormat(localeRef.value, options).format(numericValue)
  },
  locale: localeRef,
  te: (key: string) => messages[key] !== undefined,
  tm: (key: string) => messages[key] ?? key,
}))

mockNuxtImport('useId', () => () => 'compare-hero')

const route = reactive({ hash: '#compare=1234567890123', query: {} })
const routerReplace = vi.fn()

mockNuxtImport('useRoute', () => () => route)
mockNuxtImport('useRouter', () => () => ({
  replace: routerReplace,
  resolve: (to: unknown) => {
    if (typeof to === 'string') {
      return { href: to }
    }

    if (
      to &&
      typeof to === 'object' &&
      'path' in to &&
      typeof to.path === 'string'
    ) {
      return { href: to.path }
    }

    if (
      to &&
      typeof to === 'object' &&
      'href' in to &&
      typeof to.href === 'string'
    ) {
      return { href: to.href }
    }

    return { href: '' }
  },
}))

vi.mock('~/composables/usePluralizedTranslation', () => ({
  usePluralizedTranslation: () => ({
    translatePlural: (_key: string, count: number) => `${count} produits`,
  }),
}))

vi.mock('~~/shared/utils/localized-routes', () => ({
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
          props: {
            icon: { type: String, default: '' },
            size: { type: [Number, String], default: 24 },
          },
          setup(props) {
            return () =>
              h(
                'span',
                { class: 'v-icon-stub', 'data-icon': props.icon },
                props.icon
              )
          },
        }),
        VAlert: simpleStub('div'),
        VProgressLinear: simpleStub('div'),
        VBtn: defineComponent({
          name: 'VBtnStub',
          props: { type: { type: String, default: 'button' } },
          setup(props, { slots, attrs }) {
            return () =>
              h('button', { ...attrs, type: props.type }, slots.default?.())
          },
        }),
        VTooltip: defineComponent({
          name: 'VTooltipStub',
          props: {
            text: { type: String, default: '' },
            location: { type: String, default: '' },
          },
          setup(_props, { slots }) {
            const activator = slots.activator?.({ props: {} })
            const content = slots.default?.()
            return () =>
              h('div', { class: 'v-tooltip-stub' }, [activator, content])
          },
        }),
        ClientOnly: simpleStub('div'),
        NuxtLink: defineComponent({
          name: 'NuxtLinkStub',
          props: {
            to: { type: [String, Object], default: '' },
            custom: { type: Boolean, default: false },
          },
          setup(props, { slots, attrs }) {
            const resolveHref = () => {
              if (typeof props.to === 'string') {
                return props.to
              }

              if (props.to && typeof props.to === 'object') {
                if ('path' in props.to && typeof props.to.path === 'string') {
                  return props.to.path
                }

                if ('href' in props.to && typeof props.to.href === 'string') {
                  return props.to.href
                }
              }

              return ''
            }

            if (props.custom) {
              return () =>
                slots.default?.({
                  href: resolveHref(),
                  navigate: vi.fn(),
                  isActive: false,
                  isExactActive: false,
                })
            }

            return () =>
              h('a', { ...attrs, href: resolveHref() }, slots.default?.())
          },
        }),
        NuxtImg: defineComponent({
          name: 'NuxtImgStub',
          props: {
            alt: { type: String, default: '' },
            src: { type: String, default: '' },
          },
          setup(props) {
            return () => h('img', { alt: props.alt, src: props.src })
          },
        }),
      },
    },
  })
}

const createEntry = (
  overrides: Partial<CompareProductEntryLike> = {}
): CompareProductEntryLike => ({
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
    loadProductsMock.mockResolvedValue([
      createEntry({ verticalId: 'vertical-1' }),
    ])
    loadVerticalMock.mockResolvedValue({
      verticalHomeTitle: 'Lave-vaisselle',
      verticalHomeUrl: 'electromenager/lave-vaisselle',
    })

    const wrapper = await mountPage()
    await flushPromises()
    await flushPromises()

    expect(wrapper.get('.page-header__title').text()).toBe(
      'Comparateur de lave-vaisselle'
    )
    expect(wrapper.get('.page-header__subtitle').text()).toBe(
      'Analysez les fiches techniques, les prix et les indicateurs écologiques des lave-vaisselle sélectionnés.'
    )

    const backLink = wrapper.get('.compare-page__hero-back')
    expect(backLink.text()).toContain('Retour aux lave-vaisselle')
    expect(backLink.attributes('to')).toBe('/electromenager/lave-vaisselle')
  })

  it('falls back to generic hero copy when the vertical configuration is unavailable', async () => {
    loadProductsMock.mockResolvedValue([createEntry({ verticalId: null })])
    loadVerticalMock.mockResolvedValue(null)

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('.page-header__title').text()).toBe(
      'Comparateur de produits'
    )
    expect(wrapper.get('.page-header__subtitle').text()).toBe(
      'Analysez les fiches techniques, les prix et les indicateurs écologiques des produits sélectionnés.'
    )
    expect(wrapper.find('.compare-page__hero-back').exists()).toBe(false)
  })
})

describe('Ecological scores', () => {
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

  it('displays relative ecological scores with up to two decimals and highlights the best values', async () => {
    loadProductsMock.mockResolvedValue([
      createEntry({
        gtin: '0001',
        product: {
          identity: { brand: 'Brand A', model: 'Model A' },
          base: {},
          offers: {},
          scores: {
            ecoscore: { value: 4.5, relativ: { value: 3.456 } },
            scores: {
              BRAND_SUSTAINABILITY: { value: 1.5, relativ: { value: 2.1 } },
              DATA_QUALITY: { relativ: { value: 4.789 } },
            },
          },
          attributes: {},
        },
        title: 'Produit A',
        brand: 'Brand A',
        model: 'Model A',
      }),
      createEntry({
        gtin: '0002',
        product: {
          identity: { brand: 'Brand B', model: 'Model B' },
          base: {},
          offers: {},
          scores: {
            ecoscore: { value: 2.1, relativ: { value: 4.3 } },
            scores: {
              BRAND_SUSTAINABILITY: { value: 4.0, relativ: { value: 4.123 } },
              DATA_QUALITY: { relativ: { value: 3.5 } },
            },
          },
          attributes: {},
        },
        title: 'Produit B',
        brand: 'Brand B',
        model: 'Model B',
      }),
    ])

    loadVerticalMock.mockResolvedValue(null)

    const wrapper = await mountPage()
    await flushPromises()
    await flushPromises()

    const ecologicalSection = wrapper
      .findAll('.compare-section')
      .find(section =>
        section
          .findAll('.compare-section__title')
          .some(title => title.text() === 'Impact écologique')
      )

    expect(ecologicalSection).toBeDefined()

    const rows = ecologicalSection!.findAll('.compare-grid__row')
    const ecoscoreRow = rows.find(
      row => row.find('.compare-grid__feature-label')?.text() === 'Écoscore'
    )

    expect(ecoscoreRow).toBeTruthy()

    const ecoscoreCells = ecoscoreRow!.findAll('.compare-grid__value')
    expect(ecoscoreCells).toHaveLength(2)
    expect(ecoscoreCells[0].text()).toContain('4,5')
    expect(ecoscoreCells[1].text()).toContain('2,1')
    expect(ecoscoreCells[0].classes()).toContain(
      'compare-grid__value--highlight'
    )
    expect(ecoscoreCells[1].classes()).not.toContain(
      'compare-grid__value--highlight'
    )

    const brandRow = rows.find(
      row =>
        row.find('.compare-grid__feature-label')?.text() === 'Durabilité marque'
    )

    expect(brandRow).toBeTruthy()

    const brandCells = brandRow!.findAll('.compare-grid__value')
    expect(brandCells[0].text()).toContain('1,5')
    expect(brandCells[1].text()).toContain('4')
    expect(brandCells[1].classes()).toContain('compare-grid__value--highlight')
  })
})


describe('Comparison integrity states', () => {
  beforeEach(() => {
    loadProductsMock.mockReset()
    loadVerticalMock.mockReset()
    hasMixedVerticalsMock.mockReset()
    compareStore.clear.mockReset()
    compareStore.addProduct.mockReset()
    compareStore.removeById.mockReset()
    routerReplace.mockReset()
    route.hash = '#compare=1234567890123Vs9999999999999'
    localeRef.value = 'fr-FR'
    hasMixedVerticalsMock.mockReturnValue(false)
  })

  it('renders an explicit section heading and provenance metadata for compared products', async () => {
    loadProductsMock.mockResolvedValue([
      createEntry({
        gtin: '1234567890123',
        product: {
          identity: { brand: 'Brand', model: 'Model' },
          base: { lastChange: 1735689600000 },
          offers: {},
          scores: {},
          attributes: {},
          datasources: { datasourceCodes: { amazon: 10, icecat: 20 } },
        },
      }),
      createEntry({
        gtin: '9999999999999',
        title: 'Produit 2',
        model: 'Model 2',
      }),
    ])
    loadVerticalMock.mockResolvedValue(null)

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('Comparaison de 2 produits sélectionnés')
    expect(wrapper.text()).toContain('Source')
    expect(wrapper.text()).toContain('amazon, icecat')
    expect(wrapper.text()).toContain('Dernière mise à jour')
  })

  it('shows a clear invalid state when deduplication leaves fewer than two products', async () => {
    loadProductsMock.mockResolvedValue([
      createEntry({
        gtin: '1234567890123',
        product: { gtin: 111, identity: { brand: 'A', model: 'A' }, base: {}, offers: {}, scores: {}, attributes: {} },
      }),
      createEntry({
        gtin: '9999999999999',
        product: { gtin: 111, identity: { brand: 'A', model: 'A' }, base: {}, offers: {}, scores: {}, attributes: {} },
      }),
    ])
    loadVerticalMock.mockResolvedValue(null)

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain(
      'La comparaison nécessite au moins deux produits différents.'
    )
    expect(wrapper.find('.compare-grid').exists()).toBe(false)
  })
})
