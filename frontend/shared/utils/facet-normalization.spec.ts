import { describe, expect, it } from 'vitest'
import {
  hasRenderableFacetLabel,
  normalizeFacetLabel,
  resolveFacetUnit,
} from './facet-normalization'

describe('normalizeFacetLabel', () => {
  it('normalizes accents, case and plurals', () => {
    expect(normalizeFacetLabel('  Télévisions  ')).toBe('television')
  })

  it('applies synonym mapping', () => {
    expect(normalizeFacetLabel('occasion')).toBe('used')
  })
})

describe('hasRenderableFacetLabel', () => {
  it('rejects empty labels', () => {
    expect(hasRenderableFacetLabel('   ')).toBe(false)
  })
})

describe('resolveFacetUnit', () => {
  it('returns units for known mappings', () => {
    expect(resolveFacetUnit('attributes.ENERGY_CONSUMPTION_ANNUAL')).toBe(
      'kWh/an'
    )
  })
})
