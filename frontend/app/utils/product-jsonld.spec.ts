import { describe, expect, it } from 'vitest'
import { buildProductJsonLdGraph, compactJsonLd } from './product-jsonld'

const baseInput = {
  product: {
    gtin: 1234567890123,
    names: {
      prettyName: 'Sample Product',
    },
  },
  productTitle: 'Sample Product',
  canonicalUrl: 'https://nudger.test/products/sample',
  locale: 'fr-FR',
  breadcrumbs: [{ title: 'Home', link: '/' }],
  site: { url: 'https://nudger.test', name: 'Nudger' },
}

describe('compactJsonLd', () => {
  it('removes empty values while preserving meaningful data', () => {
    const value = compactJsonLd({
      name: 'Test',
      empty: '',
      emptyArray: [],
      emptyObject: {},
      nested: {
        keep: 0,
        remove: undefined,
      },
    })

    expect(value).toEqual({
      name: 'Test',
      nested: {
        keep: 0,
      },
    })
  })
})

describe('buildProductJsonLdGraph', () => {
  it('omits optional entries when data is missing', () => {
    const result = buildProductJsonLdGraph({
      ...baseInput,
      product: {
        gtin: 1234567890123,
        names: {
          prettyName: 'Sample Product',
        },
        identity: {
          brand: '',
        },
        attributes: {
          referentialAttributes: {},
          indexedAttributes: {},
        },
      },
    })

    expect(result).not.toBeNull()

    const graph = result?.['@graph'] as Array<Record<string, unknown>>
    const productEntry = graph.find(
      entry => entry['@type'] === 'Product'
    )

    expect(productEntry?.brand).toBeUndefined()
    expect(productEntry?.offers).toBeUndefined()
    expect(productEntry?.hasEnergyConsumptionDetails).toBeUndefined()
  })

  it('builds aggregate offers with new and occasion lists', () => {
    const result = buildProductJsonLdGraph({
      ...baseInput,
      product: {
        gtin: 1234567890123,
        names: {
          prettyName: 'Sample Product',
        },
        offers: {
          offersCount: 2,
          offersByCondition: {
            NEW: [
              {
                url: 'https://shop.example/new',
                price: 10,
                currency: 'EUR',
                condition: 'NEW',
                datasourceName: 'Shop',
              },
            ],
            OCCASION: [
              {
                url: 'https://shop.example/used',
                price: 8,
                currency: 'EUR',
                condition: 'OCCASION',
                datasourceName: 'Used Shop',
              },
            ],
          },
        },
      },
    })

    expect(result).not.toBeNull()

    const graph = result?.['@graph'] as Array<Record<string, unknown>>
    const productEntry = graph.find(
      entry => entry['@type'] === 'Product'
    ) as Record<string, unknown>

    const aggregate = productEntry?.offers as Record<string, unknown>
    expect(aggregate).toBeDefined()
    expect(aggregate?.offerCount).toBe(2)
    expect(aggregate?.lowPrice).toBe(8)
    expect(aggregate?.highPrice).toBe(10)

    const offers = aggregate?.offers as Array<Record<string, unknown>>
    expect(offers).toHaveLength(2)
    expect(offers[0]?.itemCondition).toBe(
      'https://schema.org/NewCondition'
    )
    expect(offers[1]?.itemCondition).toBe(
      'https://schema.org/UsedCondition'
    )
  })

  it('resolves additional properties from normalized attribute aliases', () => {
    const result = buildProductJsonLdGraph({
      ...baseInput,
      product: {
        gtin: 1234567890123,
        names: {
          prettyName: 'Sample Product',
        },
        attributes: {
          referentialAttributes: {
            "RESOLUTION DE L'ECRAN": '3840 x 2160',
            QUANTITEDEPORTSUSB20: '2',
            NOMDELACOULEUR: 'Noir',
          },
          indexedAttributes: {
            WEIGHT: { numericValue: 12.5 },
            HEIGHT: { numericValue: 720 },
          },
        },
      },
    })

    const graph = result?.['@graph'] as Array<Record<string, unknown>>
    const productEntry = graph.find(
      entry => entry['@type'] === 'Product'
    ) as Record<string, unknown>

    expect(productEntry?.color).toBe('Noir')
    expect((productEntry?.weight as Record<string, unknown>)?.value).toBe(12.5)
    expect((productEntry?.height as Record<string, unknown>)?.value).toBe(720)

    const additionalProperty = (productEntry?.additionalProperty ?? []) as Array<
      Record<string, unknown>
    >
    const resolutionEntry = additionalProperty.find(
      entry => entry.name === 'Résolution'
    )
    const usbEntry = additionalProperty.find(entry => entry.name === 'Ports USB')

    expect(resolutionEntry?.value).toBe('3840 x 2160')
    expect(usbEntry?.value).toBe('2')
  })
})
