import { describe, expect, it } from 'vitest'
import { resolveOfferShippingLabels } from './_offer-shipping'

const t = (key: string, ...args: unknown[]) => {
  const params = (args[0] ?? {}) as Record<string, unknown>
  if (key.endsWith('.free')) return 'Livraison offerte'
  if (key.endsWith('.cost')) return `Livraison ${params.amount}`
  return `Livrée sous ${params.count} jours`
}
const n = (value: number, options?: Intl.NumberFormatOptions) =>
  new Intl.NumberFormat('fr-FR', options).format(value)

describe('resolveOfferShippingLabels', () => {
  it('affiche frais et délai quand ils sont connus', () => {
    const labels = resolveOfferShippingLabels(
      { shippingCost: 4.99, shippingTimeDays: 3, currency: 'EUR' },
      t,
      n
    )
    expect(labels.cost).toMatch(/^Livraison 4,99\s€$/)
    expect(labels.delay).toBe('Livrée sous 3 jours')
  })

  it('affiche « offerte » quand les frais valent 0', () => {
    const labels = resolveOfferShippingLabels(
      { shippingCost: 0, currency: 'EUR' },
      t,
      n
    )
    expect(labels.cost).toBe('Livraison offerte')
    expect(labels.delay).toBeNull()
  })

  it("n'affiche rien quand les données sont absentes", () => {
    for (const offer of [
      {},
      { shippingCost: null, shippingTimeDays: null, currency: 'EUR' },
      { shippingCost: Number.NaN, shippingTimeDays: -1 },
      { shippingCost: 4.99 },
      undefined,
    ]) {
      expect(resolveOfferShippingLabels(offer, t, n)).toEqual({
        cost: null,
        delay: null,
      })
    }
  })
})
