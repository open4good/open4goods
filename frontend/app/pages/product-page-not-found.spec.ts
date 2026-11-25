import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { createError } from 'h3'

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

const routeMock = {
  params: {
    slug: ['4006381333931-inexistant'],
  },
  path: '/4006381333931-inexistant',
}

const useAsyncDataMock = vi.hoisted(() => vi.fn())

mockNuxtImport('useI18n', () => () => ({
  t: (key: string) => translate(key),
  locale: { value: 'fr-FR' },
}))

mockNuxtImport('useRoute', () => () => routeMock)
mockNuxtImport('useRequestURL', () => () => new URL('https://nudger.test/'))
mockNuxtImport('useRuntimeConfig', () => () => ({
  public: { hcaptchaSiteKey: 'key' },
}))
mockNuxtImport('useSeoMeta', () => vi.fn())
mockNuxtImport('useHead', () => vi.fn())
mockNuxtImport('navigateTo', () => vi.fn())
mockNuxtImport('useAsyncData', () => useAsyncDataMock)

const requestEventMock = vi.hoisted(() => ({ value: undefined as unknown }))
mockNuxtImport('useRequestEvent', () => () => requestEventMock.value)
vi.mock('~/utils/ssr-backend-error', () => ({
  createBackendUnavailableError: (cause: unknown) =>
    Object.assign(new Error('Service Unavailable'), {
      statusCode: 503,
      cause,
    }),
}))

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

describe('ProductPage SSR 404 (GOU-472)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    selectCategoryBySlugMock.mockReset()
    selectCategoryBySlugMock.mockRejectedValue(new Error('Category not found'))
    useAsyncDataMock.mockReset()
    requestEventMock.value = {}
    routeMock.params.slug = ['4006381333931-inexistant']
    routeMock.path = '/4006381333931-inexistant'
  })

  it('rethrows a fatal 404 when the backend reports an unknown GTIN', async () => {
    useAsyncDataMock.mockResolvedValue({
      data: ref(null),
      pending: ref(false),
      error: ref(
        createError({ statusCode: 404, statusMessage: 'Product not found' })
      ),
    })

    await expect(mountProductPage()).rejects.toMatchObject({
      statusCode: 404,
      fatal: true,
    })
  })

  it('answers a 503, not a 200 banner, when the backend fails with a 5xx during SSR', async () => {
    useAsyncDataMock.mockResolvedValue({
      data: ref(null),
      pending: ref(false),
      error: ref(
        createError({ statusCode: 500, statusMessage: 'Internal error' })
      ),
    })

    await expect(mountProductPage()).rejects.toMatchObject({
      statusCode: 503,
    })
  })

  it('answers a real 404 for a placeholder GTIN without calling the backend', async () => {
    routeMock.params.slug = ['0000000000000-fiche-factice']
    routeMock.path = '/0000000000000-fiche-factice'

    await expect(mountProductPage()).rejects.toMatchObject({
      statusCode: 404,
    })
    expect(useAsyncDataMock).not.toHaveBeenCalled()
  })
})
