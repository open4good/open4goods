import { describe, expect, it } from 'vitest'

import type {
  GlobalSearchResponseDto,
  ProductDto,
  ProductSearchResponseDto,
} from '~~/shared/api-client'

import {
  slimGlobalSearchResponse,
  slimProductSearchResponse,
  slimSearchProduct,
} from './_search-result-payload'

const buildProduct = (gtin: number): ProductDto => ({
  gtin,
  slug: `${gtin}-tv`,
  fullSlug: `/televiseurs/${gtin}-tv`,
  base: { gtin, vertical: 'tv', bestName: 'LG OLED55C3' },
  identity: { brand: 'LG', model: 'OLED55C3' },
  names: { displayName: 'LG OLED55C3' },
  attributes: {
    referentialAttributes: { BRAND: 'LG' },
    indexedAttributes: { DIAGONAL: { name: 'Diagonale', value: '55' } },
    allAttributes: { WEIGHT: { name: 'Poids', value: '18 kg' } },
  },
  resources: {
    coverImagePath: '/images/cover.webp',
    externalCover: 'https://example.org/cover.jpg',
    images: [{ url: '/images/1.webp' }, { url: '/images/2.webp' }],
    videos: [{ url: '/videos/1.mp4' }],
    pdfs: [{ url: '/pdfs/1.pdf' }],
  },
  datasources: {},
  eprel: {},
  timeline: {},
  scores: {
    scores: {
      ECOSCORE: { value: 12.5 },
      REPAIRABILITY_INDEX: { value: 7 },
    },
    impactScore: { status: 'RATED' },
  },
  offers: {
    offersCount: 2,
    bestPrice: { price: 1404.57, datasourceName: 'darty.com' },
    offersByCondition: {
      NEW: [
        { price: 1404.57, datasourceName: 'darty.com' },
        { price: 1499, datasourceName: 'fnac.com' },
      ],
    },
    newHistory: { entries: [] },
    occasionHistory: { entries: [] },
    newTrend: { trend: 'DOWN' },
  },
})

describe('slimSearchProduct', () => {
  it('keeps every field a search result card renders', () => {
    const slim = slimSearchProduct(buildProduct(8806098241927))

    expect(slim.gtin).toBe(8806098241927)
    expect(slim.fullSlug).toBe('/televiseurs/8806098241927-tv')
    expect(slim.base?.bestName).toBe('LG OLED55C3')
    expect(slim.identity?.brand).toBe('LG')
    expect(slim.names?.displayName).toBe('LG OLED55C3')
    expect(slim.attributes?.referentialAttributes).toEqual({ BRAND: 'LG' })
    expect(slim.attributes?.indexedAttributes?.DIAGONAL?.value).toBe('55')
    expect(slim.resources?.coverImagePath).toBe('/images/cover.webp')
    expect(slim.resources?.externalCover).toBe('https://example.org/cover.jpg')
    expect(slim.resources?.images).toEqual([{ url: '/images/1.webp' }])
    expect(slim.scores?.scores?.ECOSCORE?.value).toBe(12.5)
    expect(slim.scores?.impactScore?.status).toBe('RATED')
    expect(slim.offers?.offersCount).toBe(2)
    expect(slim.offers?.bestPrice?.price).toBe(1404.57)
    expect(slim.offers?.offersByCondition?.NEW).toHaveLength(2)
    expect(slim.offers?.newTrend).toEqual({ trend: 'DOWN' })
  })

  it('drops the heavy fields no result card reads', () => {
    const slim = slimSearchProduct(buildProduct(8806098241927))

    expect(slim.attributes).not.toHaveProperty('allAttributes')
    expect(slim.offers).not.toHaveProperty('newHistory')
    expect(slim.offers).not.toHaveProperty('occasionHistory')
    expect(slim.resources).not.toHaveProperty('videos')
    expect(slim.resources).not.toHaveProperty('pdfs')
    expect(slim.scores?.scores).not.toHaveProperty('REPAIRABILITY_INDEX')
    expect(slim).not.toHaveProperty('eprel')
    expect(slim).not.toHaveProperty('timeline')
    expect(slim).not.toHaveProperty('datasources')
  })

  it('does not mutate the source product', () => {
    const product = buildProduct(1)
    slimSearchProduct(product)

    expect(product.attributes?.allAttributes).toBeDefined()
    expect(product.resources?.images).toHaveLength(2)
    expect(product.offers?.newHistory).toBeDefined()
  })

  it('handles a product without optional components', () => {
    expect(slimSearchProduct({ gtin: 2 })).toEqual({ gtin: 2 })
  })
})

describe('slimGlobalSearchResponse', () => {
  it('slims products in vertical groups and missing-vertical results', () => {
    const response: GlobalSearchResponseDto = {
      verticalGroups: [
        { verticalId: 'tv', results: [{ product: buildProduct(1), score: 3 }] },
      ],
      missingVerticalResults: [{ product: buildProduct(2), score: 1 }],
      missingVerticalPage: { totalElements: 1, totalPages: 1 },
      accessoryResults: [{ product: buildProduct(3), score: 1 }],
    }

    const slim = slimGlobalSearchResponse(response)

    expect(slim?.verticalGroups?.[0]?.verticalId).toBe('tv')
    expect(slim?.verticalGroups?.[0]?.results?.[0]?.score).toBe(3)
    expect(
      slim?.verticalGroups?.[0]?.results?.[0]?.product?.attributes
    ).not.toHaveProperty('allAttributes')
    expect(
      slim?.missingVerticalResults?.[0]?.product?.offers
    ).not.toHaveProperty('newHistory')
    expect(slim?.accessoryResults?.[0]?.product?.offers).not.toHaveProperty(
      'newHistory'
    )
    expect(slim?.missingVerticalPage).toEqual({
      totalElements: 1,
      totalPages: 1,
    })
  })

  it('returns null when no search ran', () => {
    expect(slimGlobalSearchResponse(null)).toBeNull()
  })
})

describe('slimProductSearchResponse', () => {
  it('slims listed products and keeps aggregations', () => {
    const response: ProductSearchResponseDto = {
      products: {
        page: { totalElements: 1 },
        data: [buildProduct(1)],
      },
      aggregations: [{ name: 'offersCount', field: 'offersCount' }],
    }

    const slim = slimProductSearchResponse(response)

    expect(slim?.products?.page?.totalElements).toBe(1)
    expect(slim?.products?.data?.[0]).not.toHaveProperty('eprel')
    expect(slim?.aggregations).toEqual(response.aggregations)
  })

  it('returns the response untouched when it has no products', () => {
    const response: ProductSearchResponseDto = { aggregations: [] }

    expect(slimProductSearchResponse(response)).toBe(response)
    expect(slimProductSearchResponse(null)).toBeNull()
  })
})
