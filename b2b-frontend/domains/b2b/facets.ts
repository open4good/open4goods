export interface FacetSampleFixture {
  gtin: string
  billable: boolean
  creditsConsumed: number
  reason: string
  data: Record<string, unknown> | null
}

export interface FacetExample {
  labelKey: string
  gtin: string
  billable: boolean
}

export interface FacetDescriptor {
  /** e.g. "product.price" */
  id: string
  /** API endpoint path template, e.g. "/api/v1/products/{gtin}/price" */
  endpointPath: string
  /** Playground proxy endpoint, e.g. "/api/v1/customer/playground/products/price" */
  playgroundEndpoint: string
  /** Credit cost (billable calls) */
  credits: number
  /** URL slug, e.g. "products/price" */
  docSlug: string
  /** Sample mode canned fixtures keyed by GTIN */
  sampleFixtures: Record<string, Omit<FacetSampleFixture, 'gtin'>>
  /** Examples shown in the playground examples rail */
  examples: FacetExample[]
}

const SAMPLE_REQUEST_ID_BASE = 'req_sample_0'

export const FACET_PRICE: FacetDescriptor = {
  id: 'product.price',
  endpointPath: '/api/v1/products/{gtin}/price',
  playgroundEndpoint: '/api/v1/customer/playground/products/price',
  credits: 5,
  docSlug: 'products/price',
  sampleFixtures: {
    '0885909950805': {
      billable: true,
      creditsConsumed: 5,
      reason: 'fresh-offer',
      data: {
        bestPrice: { price: 699.0, currency: 'EUR', merchant: 'TechStore FR', condition: 'new' },
        offers: [
          { price: 699.0, currency: 'EUR', merchant: 'TechStore FR', condition: 'new', lastSeenDays: 1 },
          { price: 739.0, currency: 'EUR', merchant: 'BigBox Online', condition: 'new', lastSeenDays: 4 }
        ],
        offerCount: 2,
        freshness: { oldestOfferDays: 4, newestOfferDays: 1, windowDays: 30 }
      }
    },
    '0194253408994': {
      billable: false,
      creditsConsumed: 0,
      reason: 'stale-data',
      data: null
    },
    '0000000000000': {
      billable: false,
      creditsConsumed: 0,
      reason: 'product-not-found',
      data: null
    }
  },
  examples: [
    { labelKey: 'playground.example.fresh', gtin: '0885909950805', billable: true },
    { labelKey: 'playground.example.stale', gtin: '0194253408994', billable: false },
    { labelKey: 'playground.example.notFound', gtin: '0000000000000', billable: false },
    { labelKey: 'playground.example.invalid', gtin: '12345', billable: false }
  ]
}

export const FACET_PRICE_HISTORY: FacetDescriptor = {
  id: 'product.price-history',
  endpointPath: '/api/v1/products/{gtin}/price/history',
  playgroundEndpoint: '/api/v1/customer/playground/products/price-history',
  credits: 8,
  docSlug: 'products/price-history',
  sampleFixtures: {
    '0885909950805': {
      billable: true,
      creditsConsumed: 8,
      reason: 'has-history',
      data: {
        gtin: '0885909950805',
        from: '2026-05-16T00:00:00Z',
        to: '2026-06-15T00:00:00Z',
        granularity: 'DAY',
        series: [
          {
            provider: 'Merchant Feed Partner',
            condition: 'NEW',
            currency: 'EUR',
            dayPoints: [
              { date: '2026-06-14', minAmount: 749.0, maxAmount: 819.99, closeAmount: 799.99, offerCount: 3 }
            ],
            changePoints: null
          }
        ],
        nextCursor: null
      }
    },
    '0194253408994': {
      billable: false,
      creditsConsumed: 0,
      reason: 'no-price-history',
      data: null
    },
    '0000000000000': {
      billable: false,
      creditsConsumed: 0,
      reason: 'product-not-found',
      data: null
    }
  },
  examples: [
    { labelKey: 'playground.example.fresh', gtin: '0885909950805', billable: true },
    { labelKey: 'playground.example.stale', gtin: '0194253408994', billable: false },
    { labelKey: 'playground.example.notFound', gtin: '0000000000000', billable: false },
    { labelKey: 'playground.example.invalid', gtin: '12345', billable: false }
  ]
}

/** Registry of all known facets. Add new entries here as facets ship. */
export const FACETS: Record<string, FacetDescriptor> = {
  'product.price': FACET_PRICE,
  'product.price-history': FACET_PRICE_HISTORY
}

export function buildSampleResponse (facet: FacetDescriptor, gtin: string, creditsRemaining = 2495, index = 1) {
  const fixture = facet.sampleFixtures[gtin]
  if (fixture) {
    return {
      meta: {
        requestId: `${SAMPLE_REQUEST_ID_BASE}${index}`,
        gtin,
        facet: facet.id,
        billable: fixture.billable,
        creditsConsumed: fixture.creditsConsumed,
        creditsRemaining: fixture.billable ? creditsRemaining - fixture.creditsConsumed : creditsRemaining,
        reason: fixture.reason,
        responseTimeMs: Math.floor(Math.random() * 40) + 8
      },
      data: fixture.data
    }
  }
  return {
    meta: {
      requestId: `${SAMPLE_REQUEST_ID_BASE}${index + 1}`,
      gtin,
      facet: facet.id,
      billable: false,
      creditsConsumed: 0,
      creditsRemaining,
      reason: 'product-not-found',
      responseTimeMs: 9
    },
    data: null
  }
}
