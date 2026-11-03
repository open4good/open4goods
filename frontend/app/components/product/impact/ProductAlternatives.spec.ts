import { mountSuspended } from '@nuxt/test-utils/runtime'
import { useNuxtApp } from '#imports'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'
import ProductAlternatives from './ProductAlternatives.vue'
import { flushPromises } from '@vue/test-utils'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

vi.mock('~/components/category/products/CategoryProductCardGrid.vue', () => ({
  default: {
    name: 'CategoryProductCardGridStub',
    props: {
      products: Array,
      popularAttributes: Array,
      size: String,
      variant: String,
    },
    template:
      '<div class="category-product-card-grid-stub"><a v-for="item in products" :key="item.gtin" :href="item.fullSlug">{{ item.gtin }}</a></div>',
  },
}))

describe('ProductAlternatives', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            alternatives: {
              title: 'Greener alternatives',
              subtitle: 'Discover better options.',
              bestProduct: 'Best product message.',
              error: 'Error loading alternatives.',
              retry: 'Retry',
              untitled: 'Untitled product',
              viewProduct: 'View product {name}',
              filters: {
                price: 'Price ≤ yours',
                ecoscore: 'Eco-score ≥ yours',
              },
              tooltips: {
                price: 'Price tooltip {value}',
                ecoscore: 'Ecoscore tooltip {value}',
                attributeBetter: 'Better {label}',
                attributeLower: 'Lower {label}',
                attributeEqual: 'Equal {label}',
              },
            },
          },
        },
      },
    },
  })

  const fetchMock = vi.fn()
  // globalThis.$fetch is swapped on every SSR request: the component must use
  // the client of its own nuxtApp (GOU-1226).
  const globalFetchMock = vi.fn(() =>
    Promise.reject(new Error('globalThis.$fetch must not be used'))
  )
  const nuxtApp = useNuxtApp()
  const originalFetch = nuxtApp.$fetch

  beforeEach(() => {
    fetchMock.mockResolvedValue({ products: [] })
    nuxtApp.$fetch = fetchMock as unknown as typeof nuxtApp.$fetch
    vi.stubGlobal('$fetch', globalFetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    nuxtApp.$fetch = originalFetch
    fetchMock.mockReset()
    globalFetchMock.mockClear()
    vi.restoreAllMocks()
  })

  const product: ProductDto = {
    gtin: 9876543210,
    slug: 'eco-phone-pro',
    fullSlug: '/phones/eco-phone-pro',
    base: {
      ecoscoreValue: 15,
    },
    identity: {
      bestName: 'Eco Phone Pro',
    },
    offers: {
      bestPrice: {
        price: 349.9,
        currency: 'EUR',
      },
    },
    attributes: {
      indexedAttributes: {
        POWER_CONSUMPTION: {
          numericValue: 55,
        },
      },
    },
    scores: {
      ecoscore: {
        value: 15,
      },
      scores: {
        ECOSCORE: {
          value: 15,
        },
      },
    },
  } as unknown as ProductDto

  const popularAttributes: AttributeConfigDto[] = [
    {
      key: 'POWER_CONSUMPTION',
      name: 'Power consumption',
      filteringType: 'NUMERIC',
      impactBetterIs: 'LOWER',
    } as AttributeConfigDto,
  ]

  it('fetches alternatives with default filters', async () => {
    fetchMock.mockResolvedValue({
      products: {
        data: [1, 2, 3].map(gtin => ({ gtin, fullSlug: `/phones/${gtin}` })),
      },
    })
    const wrapper = await mountSuspended(ProductAlternatives, {
      props: {
        product,
        verticalId: 'phones',
        popularAttributes,
      },
      global: {
        plugins: [i18n],
      },
    })

    await flushPromises()

    expect(fetchMock).toHaveBeenCalled()
    const lastCall = fetchMock.mock.calls.at(-1)
    expect(lastCall?.[0]).toBe('/api/products/search')
    const requestBody = (
      lastCall?.[1] as {
        body?: { filters?: { filters?: Array<Record<string, unknown>> } }
      }
    )?.body
    const initialFilters = requestBody?.filters?.filters ?? []
    expect(initialFilters).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          field: 'price.minPrice.price',
          operator: 'range',
          max: 349.9,
        }),
        expect.objectContaining({
          field: ECOSCORE_RELATIVE_FIELD,
          operator: 'range',
          min: 15,
        }),
        expect.objectContaining({
          field: 'attributes.indexed.POWER_CONSUMPTION.numericValue',
          operator: 'range',
          max: 55,
        }),
      ])
    )

    const priceChip = wrapper
      .findAll('.product-alternatives__chip')
      .find(chip => chip.text() === 'Price ≤ yours')
    expect(priceChip).toBeDefined()
    await priceChip?.trigger('click')

    await flushPromises()

    const updatedCall = fetchMock.mock.calls.at(-1)
    const updatedBody = (
      updatedCall?.[1] as {
        body?: { filters?: { filters?: Array<Record<string, unknown>> } }
      }
    )?.body
    const updatedFilters = updatedBody?.filters?.filters ?? []
    expect(updatedFilters).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          field: ECOSCORE_RELATIVE_FIELD,
          operator: 'range',
          min: 15,
        }),
        expect.objectContaining({
          field: 'attributes.indexed.POWER_CONSUMPTION.numericValue',
          operator: 'range',
          max: 55,
        }),
      ])
    )
    expect(updatedFilters).not.toEqual(
      expect.arrayContaining([
        expect.objectContaining({ field: 'price.minPrice.price' }),
      ])
    )
  })

  it('renders the alternatives returned by the search, without the current product', async () => {
    fetchMock.mockResolvedValue({
      products: {
        data: [
          { gtin: 1, fullSlug: '/phones/1-alt-one' },
          { gtin: product.gtin, fullSlug: product.fullSlug },
          { gtin: 2, fullSlug: '/phones/2-alt-two' },
          { gtin: 3, fullSlug: '/phones/3-alt-three' },
        ],
      },
    })

    const wrapper = await mountSuspended(ProductAlternatives, {
      props: {
        product,
        verticalId: 'phones-render',
        popularAttributes,
      },
      global: {
        plugins: [i18n],
      },
    })

    await flushPromises()

    const links = wrapper.findAll('.category-product-card-grid-stub a')
    expect(links.map(link => link.attributes('href'))).toEqual([
      '/phones/1-alt-one',
      '/phones/2-alt-two',
      '/phones/3-alt-three',
    ])
    expect(wrapper.emitted('alternativesUpdated')?.at(-1)).toEqual([
      { hasAlternatives: true, hydrated: true },
    ])
  })
  describe('relaxation of the default filters (GOU-1215)', () => {
    const makeProducts = (count: number, offset = 1) => ({
      products: {
        data: Array.from({ length: count }, (_, index) => ({
          gtin: offset + index,
          fullSlug: `/phones/${offset + index}-alt`,
        })),
      },
    })

    const filterKeysOf = (call: unknown[]): string[] => {
      const body = (
        call[1] as {
          body?: { filters?: { filters?: Array<{ field: string }> } }
        }
      ).body
      return (body?.filters?.filters ?? []).map(clause => clause.field)
    }

    const mountWith = (verticalId: string) =>
      mountSuspended(ProductAlternatives, {
        props: { product, verticalId, popularAttributes },
        global: { plugins: [i18n] },
      })

    const ATTRIBUTE = 'attributes.indexed.POWER_CONSUMPTION.numericValue'

    it('does not relax when the first search returns 3 alternatives', async () => {
      fetchMock.mockResolvedValue(makeProducts(3))
      await mountWith('phones-no-relax')
      await flushPromises()

      expect(fetchMock).toHaveBeenCalledTimes(1)
    })

    it('relaxes attributes, then price, then ecoscore until 3 results', async () => {
      fetchMock
        .mockResolvedValueOnce(makeProducts(0))
        .mockResolvedValueOnce(makeProducts(2))
        .mockResolvedValueOnce(makeProducts(3))
      const wrapper = await mountWith('phones-relax')
      await flushPromises()

      const calls = fetchMock.mock.calls
      expect(calls).toHaveLength(3)
      expect(filterKeysOf(calls[0]!)).toEqual([
        'price.minPrice.price',
        ECOSCORE_RELATIVE_FIELD,
        ATTRIBUTE,
      ])
      expect(filterKeysOf(calls[1]!)).toEqual([
        'price.minPrice.price',
        ECOSCORE_RELATIVE_FIELD,
      ])
      expect(filterKeysOf(calls[2]!)).toEqual([ECOSCORE_RELATIVE_FIELD])

      expect(
        wrapper.findAll('.category-product-card-grid-stub a')
      ).toHaveLength(3)
      const active = wrapper
        .findAll('.product-alternatives__chip--active')
        .map(chip => chip.text())
      expect(active).toEqual(['Eco-score ≥ yours'])
    })

    it('shows what exists when the category is too small', async () => {
      fetchMock
        .mockResolvedValueOnce(makeProducts(0))
        .mockResolvedValueOnce(makeProducts(0))
        .mockResolvedValueOnce(makeProducts(0))
        .mockResolvedValueOnce(makeProducts(2))
      const wrapper = await mountWith('phones-small')
      await flushPromises()

      expect(fetchMock).toHaveBeenCalledTimes(4)
      expect(filterKeysOf(fetchMock.mock.calls[3]!)).toEqual([])
      expect(
        wrapper.findAll('.category-product-card-grid-stub a')
      ).toHaveLength(2)
      expect(wrapper.text()).not.toContain('Best product message.')
    })

    it('shows the best-product message only when nothing exists', async () => {
      fetchMock.mockResolvedValue(makeProducts(0))
      const wrapper = await mountWith('phones-empty')
      await flushPromises()

      expect(wrapper.text()).toContain('Best product message.')
    })

    it('keeps relaxing across awaits with the request client (GOU-1226)', async () => {
      // Each search resolves on a later macrotask, as a real SSR request does:
      // the Nuxt context is gone when the next relaxation step is issued.
      const later = <T>(value: T) =>
        new Promise<T>(resolve => setTimeout(() => resolve(value), 0))
      fetchMock
        .mockImplementationOnce(() => later(makeProducts(1)))
        .mockImplementationOnce(() => later(makeProducts(3, 10)))
      const wrapper = await mountWith('phones-ssr-relax')
      await flushPromises()
      await new Promise(resolve => setTimeout(resolve, 0))
      await flushPromises()

      expect(fetchMock).toHaveBeenCalledTimes(2)
      expect(globalFetchMock).not.toHaveBeenCalled()
      expect(
        wrapper
          .findAll('.category-product-card-grid-stub a')
          .map(link => link.attributes('href'))
      ).toEqual(['/phones/10-alt', '/phones/11-alt', '/phones/12-alt'])
      expect(wrapper.text()).not.toContain('Error loading alternatives.')
    })

    it('keeps the alternatives already found when a relaxation step fails', async () => {
      vi.spyOn(console, 'error').mockImplementation(() => {})
      fetchMock
        .mockResolvedValueOnce(makeProducts(2))
        .mockRejectedValueOnce(new Error('[nuxt] instance unavailable'))
      const wrapper = await mountWith('phones-relax-failure')
      await flushPromises()

      expect(fetchMock).toHaveBeenCalledTimes(2)
      expect(
        wrapper.findAll('.category-product-card-grid-stub a')
      ).toHaveLength(2)
      expect(wrapper.text()).not.toContain('Error loading alternatives.')
      expect(wrapper.text()).not.toContain('Best product message.')
    })

    it('shows the error when nothing was found before a failure', async () => {
      vi.spyOn(console, 'error').mockImplementation(() => {})
      fetchMock
        .mockResolvedValueOnce(makeProducts(0))
        .mockRejectedValueOnce(new Error('[nuxt] instance unavailable'))
      const wrapper = await mountWith('phones-relax-failure-empty')
      await flushPromises()

      expect(fetchMock).toHaveBeenCalledTimes(2)
      expect(wrapper.text()).toContain('Error loading alternatives.')
      expect(wrapper.text()).not.toContain('Best product message.')
    })

    it('does not relax once the user clicked a chip', async () => {
      fetchMock.mockResolvedValue(makeProducts(3))
      const wrapper = await mountWith('phones-interacted')
      await flushPromises()
      fetchMock.mockReset()
      fetchMock.mockResolvedValue(makeProducts(0))

      const priceChip = wrapper
        .findAll('.product-alternatives__chip')
        .find(chip => chip.text() === 'Price ≤ yours')
      await priceChip?.trigger('click')
      await flushPromises()

      expect(fetchMock).toHaveBeenCalledTimes(1)
      expect(filterKeysOf(fetchMock.mock.calls[0]!)).toEqual([
        ECOSCORE_RELATIVE_FIELD,
        ATTRIBUTE,
      ])
      expect(wrapper.text()).toContain('Best product message.')
    })
  })
})
