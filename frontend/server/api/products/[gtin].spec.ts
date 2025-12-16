import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import ProductAttributeSourcingLabel from '~~/app/components/product/attributes/ProductAttributeSourcingLabel.vue'
import type { ProductDto } from '~~/shared/api-client'

type ProductRouteHandler = (typeof import('./[gtin]'))['default']

const getProductByGtinMock = vi.hoisted(() => vi.fn())
const useProductServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ getProductByGtin: getProductByGtinMock }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'en' as const }))
)
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())
const getRouterParamMock = vi.hoisted(() =>
  vi.fn<(event: unknown, name: string) => string | undefined>()
)
const createErrorMock = vi.hoisted(() =>
  vi.fn(
    (input: {
      statusCode: number
      statusMessage: string
      cause?: unknown
    }) => ({
      ...input,
      isCreateError: true,
    })
  )
)

const VTooltipStub = defineComponent({
  name: 'VTooltipStub',
  props: { text: { type: String, default: '' } },
  setup(props, { slots }) {
    return () =>
      h('div', { class: 'v-tooltip-stub', 'data-text': props.text }, [
        slots.activator?.({ props: {} }),
        h('div', { class: 'v-tooltip-stub__content' }, slots.default?.()),
      ])
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  props: {
    icon: { type: Boolean, default: false },
    density: { type: String, default: 'default' },
    variant: { type: String, default: 'text' },
    ariaLabel: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () =>
      h(
        'button',
        {
          class: 'v-btn-stub',
          'data-icon': props.icon,
          'data-density': props.density,
          'data-variant': props.variant,
          'aria-label': props.ariaLabel,
          type: 'button',
        },
        slots.default?.()
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: {
    icon: { type: String, default: '' },
    color: { type: String, default: '' },
  },
  setup(props) {
    return () =>
      h('i', { class: 'v-icon-stub', 'data-color': props.color }, props.icon)
  },
})

const VCardStub = defineComponent({
  name: 'VCardStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-card-stub' }, slots.default?.())
  },
})

const VTableStub = defineComponent({
  name: 'VTableStub',
  setup(_, { slots }) {
    return () => h('table', { class: 'v-table-stub' }, slots.default?.())
  },
})

const VDividerStub = defineComponent({
  name: 'VDividerStub',
  setup() {
    return () => h('hr', { class: 'v-divider-stub' })
  },
})

const VChipStub = defineComponent({
  name: 'VChipStub',
  setup(_, { slots }) {
    return () => h('span', { class: 'v-chip-stub' }, slots.default?.())
  },
})

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      product: {
        attributes: {
          sourcing: {
            bestValue: 'Best value',
            sourceCount: {
              one: '{count} source',
              other: '{count} sources',
            },
            columns: {
              source: 'Source',
              value: 'Value',
            },
            empty: 'No sourcing information available.',
            status: {
              conflicts: 'Conflicts detected',
              noConflicts: 'No conflicts detected',
            },
            tooltipAriaLabel: 'Show sourcing details',
          },
        },
      },
    },
  },
})

vi.mock('h3', () => ({
  defineEventHandler: (fn: ProductRouteHandler) => fn,
  getRouterParam: getRouterParamMock,
  createError: createErrorMock,
}))

vi.mock('~~/shared/api-client/services/products.services', () => ({
  useProductService: useProductServiceMock,
}))

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: extractBackendErrorDetailsMock,
}))

vi.mock('../../utils/cache-headers', () => ({
  setDomainLanguageCacheHeaders: setDomainLanguageCacheHeadersMock,
}))

describe('server/api/products/[gtin]', () => {
  let handler: ProductRouteHandler
  let consoleErrorSpy: ReturnType<typeof vi.spyOn>

  beforeEach(async () => {
    vi.resetModules()

    getProductByGtinMock.mockReset()
    useProductServiceMock.mockReturnValue({
      getProductByGtin: getProductByGtinMock,
    })
    resolveDomainLanguageMock.mockReturnValue({ domainLanguage: 'fr' })
    extractBackendErrorDetailsMock.mockResolvedValue({
      statusCode: 502,
      statusMessage: 'Bad Gateway',
      statusText: 'Bad Gateway',
      bodyText: undefined,
      isResponseError: false,
      logMessage: 'HTTP 502 - Bad Gateway',
    })
    getRouterParamMock.mockImplementation((event, name) => {
      const context = (
        event as {
          context?: { params?: Record<string, string | undefined> }
        }
      ).context
      return context?.params?.[name]
    })
    createErrorMock.mockImplementation(input => ({
      ...input,
      isCreateError: true,
    }))

    consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined)

    handler = (await import('./[gtin]')).default
  })

  afterEach(() => {
    consoleErrorSpy.mockRestore()
    vi.clearAllMocks()
  })

  it('fetches product data and forwards the domain language', async () => {
    const productResponse = { gtin: 1234567890123, slug: 'test-product' }
    getProductByGtinMock.mockResolvedValue(productResponse)

    const event = {
      node: {
        req: {
          headers: { host: 'nudger.example' },
        },
      },
      context: { params: { gtin: '1234567890123' } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    const response = await handler(event)

    expect(setDomainLanguageCacheHeadersMock).toHaveBeenCalledWith(
      event,
      'public, max-age=300, s-maxage=300'
    )
    expect(resolveDomainLanguageMock).toHaveBeenCalledWith('nudger.example')
    expect(useProductServiceMock).toHaveBeenCalledWith('fr')
    expect(getProductByGtinMock).toHaveBeenCalledWith(1234567890123)
    expect(response).toEqual(productResponse)
  })

  it('serialises sourcing sets to arrays consumable by the UI', async () => {
    const event = {
      node: {
        req: {
          headers: { host: 'nudger.example' },
        },
      },
      context: { params: { gtin: '9876543210987' } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    const productResponse: ProductDto = {
      gtin: 9876543210987,
      slug: 'sourcing-test-product',
      attributes: {
        indexedAttributes: {
          weight: {
            name: 'Weight',
            value: '1 kg',
            sourcing: {
              bestValue: '1 kg',
              conflicts: false,
              sources: new Set([
                {
                  datasourceName: 'icecat.biz',
                  value: '1 kg',
                },
                {
                  datasourceName: 'eprel',
                  value: '1 kg',
                },
              ]),
            },
          },
        },
        classifiedAttributes: [
          {
            name: 'General',
            attributes: [
              {
                name: 'Colour',
                value: 'Black',
                sourcing: {
                  bestValue: 'Black',
                  conflicts: false,
                  sources: new Set([
                    {
                      datasourceName: 'icecat.biz',
                      value: 'Black',
                    },
                    {
                      datasourceName: 'fnac.com',
                      value: 'Black',
                    },
                  ]),
                },
              },
            ],
          },
        ],
      },
    }

    getProductByGtinMock.mockResolvedValueOnce(productResponse)

    const response = await handler(event)

    const indexedSources =
      response.attributes?.indexedAttributes?.weight?.sourcing?.sources
    expect(Array.isArray(indexedSources)).toBe(true)
    expect(indexedSources).toEqual([
      expect.objectContaining({ datasourceName: 'icecat.biz', value: '1 kg' }),
      expect.objectContaining({ datasourceName: 'eprel', value: '1 kg' }),
    ])

    const groupedSources =
      response.attributes?.classifiedAttributes?.[0]?.attributes?.[0]?.sourcing
        ?.sources
    expect(Array.isArray(groupedSources)).toBe(true)
    expect(groupedSources).toHaveLength(2)

    const wrapper = mount(ProductAttributeSourcingLabel, {
      props: {
        sourcing:
          response.attributes?.indexedAttributes?.weight?.sourcing ?? null,
        value: '1 kg',
      },
      global: {
        plugins: [i18n],
        stubs: {
          VTooltip: VTooltipStub,
          VBtn: VBtnStub,
          VIcon: VIconStub,
          VCard: VCardStub,
          VTable: VTableStub,
          VDivider: VDividerStub,
          VChip: VChipStub,
        },
      },
    })

    const tableRows = wrapper.findAll('.v-table-stub tbody tr')
    expect(tableRows).toHaveLength(2)
    expect(tableRows[0].text()).toContain('icecat.biz')
    expect(tableRows[1].text()).toContain('eprel')
  })

  it('throws a 400 error when the GTIN parameter is missing', async () => {
    getRouterParamMock.mockReturnValueOnce(undefined)

    const event = {
      node: {
        req: {
          headers: { host: 'nudger.example' },
        },
      },
      context: { params: { gtin: undefined } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
    expect(useProductServiceMock).not.toHaveBeenCalled()
  })

  it('translates backend errors into HTTP responses', async () => {
    const backendFailure = new Error('backend boom')
    getProductByGtinMock.mockRejectedValueOnce(backendFailure)

    const event = {
      node: {
        req: {
          headers: {
            host: 'nudger.example',
          },
        },
      },
      context: { params: { gtin: '1234567890123' } },
    } as unknown as Parameters<ProductRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 502 })

    expect(extractBackendErrorDetailsMock).toHaveBeenCalledWith(backendFailure)
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Error fetching product:',
      'HTTP 502 - Bad Gateway',
      expect.any(Object)
    )
  })
})
