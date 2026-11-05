import { describe, expect, it } from 'vitest'
import { blogOfferBlocksBySlug, getBlogOfferBlocks } from './blog-offer-blocks'

describe('blog-offer-blocks config', () => {
  it('declares the three targeted articles', () => {
    expect(Object.keys(blogOfferBlocksBySlug).sort()).toEqual([
      'quel-systeme-choisir-entre-four-pyrolyse-et-catalyse',
      'quel-volume-de-four-choisir',
      'quelle-taille-de-tv-choisir-pour-son-salon',
    ])
  })

  it('maps verticals and category pages', () => {
    expect(
      getBlogOfferBlocks('quelle-taille-de-tv-choisir-pour-son-salon')
    ).toMatchObject({ vertical: 'tv', categoryPath: '/televiseurs' })
    expect(getBlogOfferBlocks('quel-volume-de-four-choisir')).toMatchObject({
      vertical: 'oven',
      categoryPath: '/fours',
    })
  })

  it('returns null for other articles', () => {
    expect(getBlogOfferBlocks('autre-article')).toBeNull()
    expect(getBlogOfferBlocks(null)).toBeNull()
  })

  it('gives every block a unique id and at least one filter', () => {
    for (const config of Object.values(blogOfferBlocksBySlug)) {
      const ids = config.blocks.map(b => b.id)
      expect(new Set(ids).size).toBe(ids.length)
      config.blocks.forEach(b => expect(b.filters.length).toBeGreaterThan(0))
    }
  })

  it('never lets a value fall in two range blocks of the same article', () => {
    for (const config of Object.values(blogOfferBlocksBySlug)) {
      const ranges = config.blocks.flatMap(b =>
        b.filters
          .filter(f => f.operator === 'range')
          .map(f => ({
            min: f.min ?? -Infinity,
            max: f.max ?? Infinity,
          }))
      )
      for (let i = 0; i < ranges.length; i++) {
        for (let j = i + 1; j < ranges.length; j++) {
          const a = ranges[i]!
          const b = ranges[j]!
          expect(Math.max(a.min, b.min)).toBeGreaterThan(
            Math.min(a.max, b.max)
          )
        }
      }
    }
  })
})
