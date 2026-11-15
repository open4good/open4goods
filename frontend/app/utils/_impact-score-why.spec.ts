import { describe, it, expect } from 'vitest'
import { computeWhyHighlights, resolveWhyVariant } from './_impact-score-why'
import type { ScoreView } from '~/components/product/impact/impact-types'

const criterion = (
  id: string,
  relativeValue: number | null,
  extra: Partial<ScoreView> = {}
): ScoreView => ({
  id,
  label: id.toLowerCase(),
  relativeValue,
  coefficient: 1,
  ...extra,
})

describe('computeWhyHighlights', () => {
  const scores: ScoreView[] = [
    criterion('ECOSCORE', 4),
    criterion('A', 5, { attributeValue: '41', attributeSuffix: 'W' }),
    criterion('B', 4),
    criterion('C', 3),
    criterion('D', 0.5),
    criterion('E', 1.5),
    criterion('F', 2),
    criterion('MISSING', null, { notFilled: true }),
  ]

  it('cites the two criteria furthest above and below the average', () => {
    const { strengths, weaknesses } = computeWhyHighlights(scores)
    expect(strengths.map(s => s.id)).toEqual(['A', 'B'])
    expect(weaknesses.map(s => s.id)).toEqual(['D', 'E'])
    expect(strengths[0]?.value).toBe('41 W')
  })

  it('lists unfilled criteria as missing, never as weaknesses', () => {
    const { weaknesses, missing } = computeWhyHighlights(scores)
    expect(missing).toEqual(['missing'])
    expect(weaknesses.map(s => s.id)).not.toContain('MISSING')
  })

  it('ignores the global score and criteria with no relative value', () => {
    const { strengths, weaknesses } = computeWhyHighlights([
      criterion('ECOSCORE', 5),
      criterion('X', null),
    ])
    expect(strengths).toEqual([])
    expect(weaknesses).toEqual([])
  })

  it('breaks ties by weight then id, deterministically', () => {
    const tied = [
      criterion('B', 4, { coefficient: 1 }),
      criterion('A', 4, { coefficient: 1 }),
      criterion('C', 4, { coefficient: 3 }),
    ]
    expect(computeWhyHighlights(tied).strengths.map(s => s.id)).toEqual([
      'C',
      'A',
    ])
  })

  it('does not cite a criterion exactly on the average', () => {
    const { strengths, weaknesses } = computeWhyHighlights([
      criterion('A', 2.5),
    ])
    expect(strengths).toEqual([])
    expect(weaknesses).toEqual([])
  })

  it('exposes the category average only when available', () => {
    const { strengths } = computeWhyHighlights([
      criterion('A', 5, {
        attributeValue: '41',
        attributeSuffix: 'W',
        absolute: { avg: 78.04 },
      }),
      criterion('B', 4),
    ])
    expect(strengths[0]?.categoryAverage).toBe('78 W')
    expect(strengths[1]?.categoryAverage).toBeNull()
  })
})

describe('resolveWhyVariant', () => {
  it('maps position levels and Non noté to variants', () => {
    expect(resolveWhyVariant({ status: 'RATED', level: 'excellent' })).toBe(
      'top'
    )
    expect(resolveWhyVariant({ status: 'RATED', level: 'good' })).toBe('middle')
    expect(resolveWhyVariant({ status: 'RATED', level: 'fair' })).toBe('middle')
    expect(resolveWhyVariant({ status: 'RATED', level: 'poor' })).toBe('bottom')
    expect(resolveWhyVariant({ status: 'NOT_RATED' })).toBe('notRated')
  })
})
