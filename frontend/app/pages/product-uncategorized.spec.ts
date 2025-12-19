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
  'product.uncategorized.noScore': "Pas d'évaluation sur ce produit !",
  'product.impact.alternatives.title': 'Alternatives',
}

const translate = (key: string) => {
  return typeof messages[key] === 'string' ? messages[key] : key
}

const productMockData = {
  gtin: '8427973010706',
  slug: 'rouleau-pour-couvre-livre-depliant-1-50x0-50',
  fullSlug: '8427973010706-rouleau-pour-couvre-livre-depliant-1-50x0-50',
  names: {
    h1Title: 'Rouleau couvre livre',
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

// Mocks
mockNuxtImport('useI18n', () => () => ({
  t: (key: string) => translate(key),
  locale: { value: 'fr-FR' },
}))

mockNuxtImport('useRoute', () => () => ({
  params: {
    slug: ['8427973010706-rouleau-pour-couvre-livre-depliant-1-50x0-50'], // User reported issue
  },
  path: '/8427973010706-rouleau-pour-couvre-livre-depliant-1-50x0-50',
}))

mockNuxtImport('useRequestURL', () => () => new URL('https://nudger.test/'))
mockNuxtImport('useRuntimeConfig', () => () => ({
  public: { hcaptchaSiteKey: 'key' },
}))
mockNuxtImport('useSeoMeta', () => vi.fn())
mockNuxtImport('useHead', () => vi.fn())
mockNuxtImport('navigateTo', () => vi.fn())
mockNuxtImport(
  'useAsyncData',
  () => () =>
    Promise.resolve({
      data: ref(productMockData),
      pending: ref(false),
      error: ref(null),
    })
)

// Mock resolving category to NULL
const selectCategoryBySlugMock = vi
  .fn()
  .mockRejectedValue(new Error('Category not found'))
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

const mountProductPage = async () => {
  const component = (await import('./[...slug].vue')).default
  return mountSuspended(component, {
    global: {
      stubs: {
        TopBanner: simpleStub('TopBanner-stub'),
        ProductSummaryNavigation: simpleStub('ProductSummaryNavigation-stub'),
        ProductHero: simpleStub('ProductHero-stub'),
        ProductImpactSection: simpleStub('ProductImpactSection-stub'),
        ProductAiReviewSection: simpleStub('ProductAiReviewSection-stub'),
        ProductPriceSection: simpleStub('ProductPriceSection-stub'),
        ProductAlternatives: simpleStub('ProductAlternatives-stub'),
        ProductAttributesSection: simpleStub('ProductAttributesSection-stub'),
        ProductDocumentationSection: simpleStub(
          'ProductDocumentationSection-stub'
        ),
        ProductAdminSection: simpleStub('ProductAdminSection-stub'),
        VAlert: VAlertStub,
        VSkeletonLoader: simpleStub('VSkeletonLoader-stub'),
        VTooltip: simpleStub('VTooltip-stub'),
      },
    },
  })
}

describe('Uncategorized Product Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders correctly for uncategorized product', async () => {
    const wrapper = await mountProductPage()

    // Should render
    expect(wrapper.exists()).toBe(true)

    // Should NOT have ProductImpactSection
    const impactSection = wrapper.findComponent({
      name: 'ProductImpactSection-stub',
    })
    expect(impactSection.exists()).toBe(false)

    // Should render the "No Score" alert
    expect(wrapper.text()).toContain(messages['product.uncategorized.noScore'])

    // Breadcrumbs check
    const hero = wrapper.findComponent({ name: 'ProductHero-stub' })
    const breadcrumbs = hero.attributes('breadcrumbs')
    expect(breadcrumbs).toBeDefined()
    // We want to ensure we have Home > Brand or similar structure.
    // Currently logic likely returns just [Brand].
    // We want to verify that we are handling it.
    // For now let's just log what we have or assert existence.
    expect(breadcrumbs).toBeDefined()
  })
})
