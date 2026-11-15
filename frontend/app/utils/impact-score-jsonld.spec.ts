import { describe, expect, it } from 'vitest'
import { buildImpactScoreMethodJsonLd } from './impact-score-jsonld'

describe('buildImpactScoreMethodJsonLd', () => {
  it('emits version and dateModified, never a rating', () => {
    const ld = buildImpactScoreMethodJsonLd({
      type: 'TechArticle',
      name: 'Impact Score',
      description: 'd',
      url: 'https://nudger.fr/impact-score',
      dateModified: '2026-11-09',
      version: '1',
      aboutName: 'Impact Score',
    })
    expect(ld['@type']).toBe('TechArticle')
    expect(ld.version).toBe('1')
    expect(ld.dateModified).toBe('2026-11-09')
    expect(JSON.stringify(ld)).not.toMatch(/Rating/)
  })

  it('omits empty optional fields', () => {
    const ld = buildImpactScoreMethodJsonLd({
      type: 'WebPage',
      name: 'n',
      description: 'd',
    })
    expect(ld).not.toHaveProperty('url')
    expect(ld).not.toHaveProperty('dateModified')
  })
})
