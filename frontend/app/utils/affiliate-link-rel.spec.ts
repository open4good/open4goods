import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

import { AFFILIATE_LINK_REL } from './_product-pricing'

const productDir = join(__dirname, '../components/product')

describe('affiliate link rel', () => {
  it('is sponsored nofollow noopener, without noreferrer', () => {
    expect(AFFILIATE_LINK_REL.split(' ').sort()).toEqual([
      'nofollow',
      'noopener',
      'sponsored',
    ])
  })

  it('is applied by every product component linking to /contrib/', () => {
    // ProductHeroPricing only feeds ProductHeroPricingPanel, which renders the links.
    const components = readdirSync(productDir).filter(
      f =>
        f.endsWith('.vue') &&
        f !== 'ProductHeroPricing.vue' &&
        /resolveOfferHref/.test(readFileSync(join(productDir, f), 'utf8'))
    )
    expect(components.length).toBeGreaterThan(0)
    for (const file of components) {
      const source = readFileSync(join(productDir, file), 'utf8')
      expect(source, file).toContain(':rel="AFFILIATE_LINK_REL"')
      expect(source, file).not.toMatch(/'noopener,noreferrer'/)
    }
  })
})
