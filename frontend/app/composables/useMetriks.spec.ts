import { readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'
import type { MetrikLatest } from '~/types/metriks'
import {
  formatMetrikValue,
  getParamsPreviousValue,
  isTopRowsMetrik,
  parseTopRows,
  topRowClicksEvolution,
} from './useMetriks'

const normalize = (s: string) => s.replace(/[\u00a0\u202f]/g, ' ')

describe('formatMetrikValue — currency', () => {
  it('formats euros for the French locale', () => {
    expect(normalize(formatMetrikValue(1234.5, 'currency', 'fr-FR'))).toBe(
      '1 234,50 €'
    )
  })

  it('formats euros for the English locale', () => {
    expect(formatMetrikValue(1234.5, 'currency', 'en-US')).toBe('€1,234.50')
  })

  it('keeps null as a dash, never as zero', () => {
    expect(formatMetrikValue(null, 'currency', 'fr-FR')).toBe('—')
  })

  it('still formats a real zero', () => {
    expect(normalize(formatMetrikValue(0, 'currency', 'fr-FR'))).toBe('0,00 €')
  })
})

describe('top rows helpers', () => {
  it('recognises the Search Console top events', () => {
    expect(isTopRowsMetrik('seo.gsc.top_queries')).toBe(true)
    expect(isTopRowsMetrik('seo.gsc.top_pages')).toBe(true)
    expect(isTopRowsMetrik('seo.gsc.clicks')).toBe(false)
  })

  it('parses rows and keeps missing previous values as null', () => {
    const rows = parseTopRows({
      rows: [
        {
          rank: 1,
          key: 'lave-linge',
          clicks: 10,
          impressions: 100,
          ctr: 10,
          position: 3.2,
          previousClicks: 8,
          previousImpressions: 90,
          previousPosition: 3.5,
        },
        {
          rank: 2,
          key: 'nouveau',
          clicks: 5,
          impressions: 50,
          ctr: 10,
          position: 7,
          previousClicks: null,
          previousImpressions: null,
          previousPosition: null,
        },
        { clicks: 1 },
      ],
    })
    expect(rows).toHaveLength(2)
    expect(rows[1]!.previousClicks).toBeNull()
  })

  it('returns no rows when params.rows is missing or invalid', () => {
    expect(parseTopRows({})).toEqual([])
    expect(parseTopRows({ rows: 'x' })).toEqual([])
  })

  it('reports "new" when the previous clicks are null, a delta otherwise', () => {
    const base = {
      rank: 1,
      key: 'k',
      clicks: 10,
      impressions: 1,
      ctr: 1,
      position: 1,
      previousImpressions: null,
      previousPosition: null,
    }
    expect(topRowClicksEvolution({ ...base, previousClicks: null })).toEqual({
      kind: 'new',
    })
    expect(topRowClicksEvolution({ ...base, previousClicks: 0 })).toEqual({
      kind: 'change',
      delta: 10,
    })
    expect(topRowClicksEvolution({ ...base, previousClicks: 14 })).toEqual({
      kind: 'change',
      delta: -4,
    })
  })

  it('reads params.previousValue only when numeric', () => {
    expect(getParamsPreviousValue({ previousValue: 1000 })).toBe(1000)
    expect(getParamsPreviousValue({ previousValue: null })).toBeNull()
    expect(getParamsPreviousValue({})).toBeNull()
  })
})

describe('open4goods-api fixture', () => {
  const latest = JSON.parse(
    readFileSync(
      path.resolve(
        __dirname,
        '../../server/fixtures/metriks/data/open4goods-api/latest.json'
      ),
      'utf-8'
    )
  ) as MetrikLatest
  const events = latest.run.events

  it('ships GSC totals with previousValue and both tops with rows', () => {
    for (const id of ['clicks', 'impressions', 'ctr', 'position']) {
      const ev = events.find(e => e.id === `seo.gsc.${id}`)
      expect(ev?.status).toBe('ok')
      expect(getParamsPreviousValue(ev!.params)).not.toBeNull()
    }
    for (const id of ['seo.gsc.top_queries', 'seo.gsc.top_pages']) {
      const ev = events.find(e => e.id === id)!
      expect(parseTopRows(ev.params).length).toBe(ev.value)
    }
  })

  it('has an affiliation network in error with a null value and a message', () => {
    const errors = events.filter(
      e => e.id.startsWith('affiliation.') && e.status === 'error'
    )
    expect(errors.length).toBeGreaterThan(0)
    for (const e of errors) {
      expect(e.value).toBeNull()
      expect(e.errorMessage).toMatch(/Credential manquant|erreur/i)
    }
    expect(
      events.some(
        e => e.id.startsWith('affiliation.effiliation.') && e.status === 'error'
      )
    ).toBe(true)
  })
})
