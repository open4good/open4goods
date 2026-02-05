import { mountSuspended } from '@nuxt/test-utils/runtime'
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
    template: '<div class="category-product-card-grid-stub"></div>',
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

  beforeEach(() => {
    fetchMock.mockResolvedValue({ products: [] })
    vi.stubGlobal('$fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    fetchMock.mockReset()
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
})
