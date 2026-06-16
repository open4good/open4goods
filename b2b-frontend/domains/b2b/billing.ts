export interface B2bBucketDetail {
  id: string
  kind: string
  creditsTotal: number
  creditsRemaining: number
  expiresAt: string | null
  catalogId: string
}

export interface B2bBalanceResponse {
  creditsRemaining: number
  buckets: B2bBucketDetail[]
}

export interface B2bTransaction {
  id: string
  type: 'GRANT' | 'DEBIT' | 'REFUND' | 'EXPIRE'
  credits: number
  facetId: string | null
  gtin: string | null
  requestId: string | null
  note: string | null
  createdAt: string
}

export interface B2bInvoice {
  id: string
  stripeInvoiceId: string
  amountCents: number
  currency: string
  status: string
  hostedInvoiceUrl: string | null
  creditsGranted: number | null
  createdAt: string
}

export interface B2bSubscription {
  id: string
  stripeSubscriptionId: string
  catalogId: string
  status: string
  currentPeriodEnd: string | null
  cancelAt: string | null
  createdAt: string
}

export interface BillingPack {
  id: string
  amountEur: number
  credits: number
  stripePriceId: string
}

export interface BillingSubscriptionPlan {
  id: string
  amountEur: number
  monthlyCredits: number
  rolloverCapMonths: number
  stripePriceId: string
}

export interface BillingCatalog {
  packs: BillingPack[]
  subscriptions: BillingSubscriptionPlan[]
}

export interface CheckoutResponse {
  url: string
}
