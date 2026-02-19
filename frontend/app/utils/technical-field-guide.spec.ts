import { describe, expect, it } from 'vitest'
import {
  buildTechnicalFieldGuideEntries,
  resolveTechnicalFieldGuideEntry,
} from './technical-field-guide'

describe('technical-field-guide', () => {
  it('resolves canonical entries and legacy aliases', () => {
    expect(resolveTechnicalFieldGuideEntry('scores.ECOSCORE.relative.value')?.mapping).toBe('scores.ECOSCORE.relative.value')
    expect(resolveTechnicalFieldGuideEntry('scores.ECOSCORE.relativ.value')?.mapping).toBe('scores.ECOSCORE.relative.value')
  })


  it('provides understandable metadata for each visible field', () => {
    const entries = buildTechnicalFieldGuideEntries([
      'gtin',
      'scores.ECOSCORE.relative.value',
      'price.minPrice.price',
    ])

    expect(entries).toHaveLength(3)
    entries.forEach(entry => {
      expect(entry.labelKey).toContain('.label')
      expect(entry.tooltipKey).toContain('.tooltip')
      expect(entry.sourceKey).toContain('.source')
    })
  })

  it('keeps essential fields first and removes duplicates', () => {
    const entries = buildTechnicalFieldGuideEntries([
      'gtin',
      'gtin',
      'scores.ECOSCORE.relative.value',
      'scores.ECOSCORE.ranking',
    ])

    expect(entries.map(entry => entry.mapping)).toEqual([
      'gtin',
      'scores.ECOSCORE.relative.value',
      'scores.ECOSCORE.ranking',
    ])
  })
})
