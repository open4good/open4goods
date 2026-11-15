import { describe, expect, it } from 'vitest'
import {
  IMPACT_SCORE_VERSIONS,
  IMPACT_SCORE_VERSIONS_LAST_MODIFIED,
} from './impact-score-versions'

describe('IMPACT_SCORE_VERSIONS', () => {
  it('has unique ids and at least one change per entry', () => {
    const ids = IMPACT_SCORE_VERSIONS.map(entry => entry.id)
    expect(new Set(ids).size).toBe(ids.length)
    expect(IMPACT_SCORE_VERSIONS.every(entry => entry.changeCount > 0)).toBe(
      true
    )
  })

  it('exposes the latest recorded date', () => {
    expect(IMPACT_SCORE_VERSIONS_LAST_MODIFIED).toBe('2026-11-09')
  })
})
