import { describe, it, expect } from 'vitest'
import type { B2bBalanceResponse, B2bTransaction, BillingCatalog } from './billing'

describe('B2bBalanceResponse', () => {
  it('recognises a valid balance response shape', () => {
    const balance: B2bBalanceResponse = {
      creditsRemaining: 2495,
      buckets: [
        {
          id: 'bucket-1',
          kind: 'FREE_GRANT',
          creditsTotal: 2500,
          creditsRemaining: 2495,
          expiresAt: null,
          catalogId: 'free',
        },
      ],
    }
    expect(balance.creditsRemaining).toBe(2495)
    expect(balance.buckets).toHaveLength(1)
    expect(balance.buckets[0]?.kind).toBe('FREE_GRANT')
  })
})

describe('B2bTransaction', () => {
  it('recognises DEBIT transaction type', () => {
    const tx: B2bTransaction = {
      id: 'tx-1',
      type: 'DEBIT',
      credits: 5,
      facetId: 'product.price',
      gtin: '0885909950805',
      requestId: 'pdreq_abc',
      note: null,
      createdAt: '2026-06-16T00:00:00Z',
    }
    expect(tx.type).toBe('DEBIT')
    expect(tx.credits).toBe(5)
  })
})

describe('BillingCatalog', () => {
  it('recognises catalog with packs and subscriptions', () => {
    const catalog: BillingCatalog = {
      packs: [{ id: 'starter', amountEur: 20, credits: 10000, stripePriceId: 'price_abc' }],
      subscriptions: [
        {
          id: 'starter',
          amountEur: 20,
          monthlyCredits: 12000,
          rolloverCapMonths: 3,
          stripePriceId: 'price_sub_abc',
        },
      ],
    }
    expect(catalog.packs).toHaveLength(1)
    expect(catalog.subscriptions[0]?.rolloverCapMonths).toBe(3)
  })
})
