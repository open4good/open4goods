import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent, h, ref } from 'vue'

const messages: Record<string, unknown> = {
  'product.banner.message': 'Banner message',
  'product.banner.cta': 'Banner CTA',
  'product.banner.ariaLabel': 'Banner Aria',
  'product.navigation.overview': 'Overview',
  'product.navigation.impact': 'Impact',
  'product.navigation.ai': 'AI Review',
  'product.navigation.price': 'Price',
  'product.navigation.alternatives': 'Alternatives',
  'product.navigation.attributes': 'Attributes',
  'product.navigation.docs': 'Documentation',
  'product.nudge.fabLabel': 'Open nudge',
  'product.nudge.fabAriaLabel': 'Open nudge aria',
}

const translate = (key: string) => {
  return typeof messages[key] === 'string' ? messages[key] : key
}

const productMockData = {
  gtin: '8427973010706',
  slug: 'rouleau-pour-couvre-livre-depliant-1-50x0-50',
  fullSlug: '8427973010706-rouleau-pour-couvre-livre-depliant-1-50x0-50',
  names: {
    pageTitle: 'Rouleau couvre livre',
  },
  identity: {
    brand: 'Editions',
    model: '1 2 3',
  },
  offers: {
    offersByCondition: {},
    bestPrice: { price: 10, currency: 'EUR' },
  },
  scores: {
    scores: {},
  },
}

const routeMock = {
  params: {
    slug: ['8427973010706-rouleau-pour-couvre-livre-depliant-1-50x0-50'],
  },
  path: '/8427973010706-rouleau-pour-couvre-livre-depliant-1-50x0-50',
}

// Capture what the relative internal fetch is called with, instead of
// mocking useAsyncData away: the fetcher must actually run so the test
// exercises the real SSR fetch call (GOU-468).
const requestFetchMock = vi.hoisted(() => vi.fn())

mockNuxtImport('useI18n', () => () => ({
  t: (key: string) => translate(key),
  locale: { value: 'fr-FR' },
}))

mockNuxtImport('useRoute', () => () => routeMock)

mockNuxtImport(
  'useRequestURL',
  () => () => new URL('https://beta.nudger.fr/')
)
mockNuxtImport('useRequestFetch', () => () => requestFetchMock)
mockNuxtImport('useRequestHeaders', () => () => ({
  host: 'beta.nudger.fr',
  'x-forwarded-host': 'beta.nudger.fr',
}))
mockNuxtImport('useRuntimeConfig', () => () => ({
  public: { hcaptchaSiteKey: 'key' },
}))
mockNuxtImport('useSeoMeta', () => vi.fn())
mockNuxtImport('useHead', () => vi.fn())
mockNuxtImport('navigateTo', () => vi.fn())
mockNuxtImport('useAsyncData', () => (_key: string, fetcher: () => Promise<unknown>) =>
  fetcher().then((data) => ({
    data: ref(data),
    pending: ref(false),
    error: ref(null),
  }))
)

const selectCategoryBySlugMock = vi.hoisted(() => vi.fn())
vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: () => ({
    selectCategoryBySlug: selectCategoryBySlugMock,
  }),
}))

const simpleStub = (name: string, tag: string = 'div') =>
  defineComponent({
    name,
    setup(_props, { slots, attrs }) {
      return () => h(tag, attrs, slots.default?.())
    },
  })

const VAlertStub = simpleStub('VAlertStub')
const VTooltipStub = defineComponent({
  name: 'VTooltipStub',
  setup(_props, { slots, attrs }) {
    return () =>
      h('div', { class: 'v-tooltip-stub', ...attrs }, [
        slots.activator?.({ props: {} }),
        slots.default?.(),
      ])
  },
})

const mountProductPage = async () => {
  const component = (await import('./[...slug].vue')).default
  return mountSuspended(component, {
    global: {
      stubs: {
        TopBanner: simpleStub('TopBanner-stub'),
        ProductSummaryNavigation: simpleStub('ProductSummaryNavigation-stub'),
        ProductHero: simpleStub('ProductHero-stub'),
        ProductImpactSection: simpleStub('ProductImpactSection-stub'),
        ProductPriceSection: simpleStub('ProductPriceSection-stub'),
        ProductAlternatives: simpleStub('ProductAlternatives-stub'),
        ProductAttributesSection: simpleStub('ProductAttributesSection-stub'),
        ProductDocumentationSection: simpleStub(
          'ProductDocumentationSection-stub'
        ),
        ProductAdminSection: simpleStub('ProductAdminSection-stub'),
        NudgeToolWizard: simpleStub('NudgeToolWizard-stub'),
        VAlert: VAlertStub,
        VSkeletonLoader: simpleStub('VSkeletonLoader-stub'),
        VTooltip: VTooltipStub,
        VDialog: simpleStub('VDialog-stub'),
        VFab: simpleStub('VFab-stub', 'button'),
      },
    },
  })
}

describe('ProductPage SSR fetch (GOU-468)', () => {
  beforeEach(() => {
    requestFetchMock.mockClear()
    requestFetchMock.mockResolvedValue(productMockData)
    selectCategoryBySlugMock.mockReset()
    selectCategoryBySlugMock.mockRejectedValue(new Error('Category not found'))
    routeMock.params.slug = [
      '8427973010706-rouleau-pour-couvre-livre-depliant-1-50x0-50',
    ]
    routeMock.path =
      '/8427973010706-rouleau-pour-couvre-livre-depliant-1-50x0-50'
  })

  it('calls the product API with a relative URL and forwards host headers', async () => {
    await mountProductPage()

    expect(requestFetchMock).toHaveBeenCalledTimes(1)
    const [url, options] = requestFetchMock.mock.calls[0] as [
      string,
      { query?: Record<string, unknown>; headers?: Record<string, unknown> },
    ]

    // Must be a relative path: no absolute origin (http(s)://…) in the URL.
    expect(url).toBe('/api/products/8427973010706')
    expect(url.startsWith('http')).toBe(false)

    expect(options.headers).toMatchObject({
      host: 'beta.nudger.fr',
      'x-forwarded-host': 'beta.nudger.fr',
    })
  })
})
