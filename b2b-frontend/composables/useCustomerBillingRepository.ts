import type { B2bBalanceResponse, B2bTransaction, B2bInvoice, B2bSubscription, CheckoutResponse } from '~/domains/b2b/billing'

export function useCustomerBillingRepository() {
  const { get, post } = useApiClient()

  const getBalance = () => get<B2bBalanceResponse>('/api/v1/customer/billing/balance')
  const getTransactions = (limit = 50) => get<B2bTransaction[]>('/api/v1/customer/billing/transactions', { limit })
  const getInvoices = () => get<B2bInvoice[]>('/api/v1/customer/billing/invoices')
  const getSubscriptions = () => get<B2bSubscription[]>('/api/v1/customer/subscriptions')
  const checkoutPack = (catalogId: string) => post<CheckoutResponse>('/api/v1/customer/billing/checkout/pack', { catalogId })
  const checkoutSubscription = (catalogId: string) => post<CheckoutResponse>('/api/v1/customer/billing/checkout/subscription', { catalogId })
  const openPortal = () => post<CheckoutResponse>('/api/v1/customer/billing/portal')

  return { getBalance, getTransactions, getInvoices, getSubscriptions, checkoutPack, checkoutSubscription, openPortal }
}
