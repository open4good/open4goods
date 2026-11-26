import { describe, expect, it } from 'vitest'

import {
  buildDatasetJsonLd,
  buildReuseSnippets,
  datasetAgeInDays,
  formatContentSize,
  formatFillRate,
  isDatasetStale,
} from './opendata'

const NOW = new Date('2026-11-26T12:00:00Z')

describe('opendata utils', () => {
  it('computes the age in days and flags files older than 8 days', () => {
    expect(datasetAgeInDays('2026-11-22T02:55:00Z', NOW)).toBe(4)
    expect(isDatasetStale('2026-11-22T02:55:00Z', NOW)).toBe(false)
    expect(isDatasetStale('2026-11-18T12:00:00Z', NOW)).toBe(false)
    expect(isDatasetStale('2026-11-17T11:00:00Z', NOW)).toBe(true)
  })

  it('never flags an unknown or invalid date', () => {
    expect(datasetAgeInDays(null, NOW)).toBeNull()
    expect(isDatasetStale(undefined, NOW)).toBe(false)
    expect(isDatasetStale('not a date', NOW)).toBe(false)
  })

  it('formats fill rates as percentages', () => {
    expect(formatFillRate(1)).toBe('100')
    expect(formatFillRate(0.4234)).toBe('42.3')
    expect(formatFillRate(0)).toBe('0')
    expect(formatFillRate(undefined)).toBeNull()
  })

  it('formats content sizes with SI units', () => {
    expect(formatContentSize(6_400_000_000)).toBe('6.4 GB')
    expect(formatContentSize(210_000_000)).toBe('210 MB')
    expect(formatContentSize(512)).toBe('512 B')
  })

  it('builds resumable download, checksum and reading snippets', () => {
    const snippets = buildReuseSnippets(
      'https://data.nudger.fr/gtin-open-data.zip',
      'abc123'
    )
    expect(snippets.curl).toBe(
      'curl -C - -L -O https://data.nudger.fr/gtin-open-data.zip'
    )
    expect(snippets.wget).toBe(
      'wget -c https://data.nudger.fr/gtin-open-data.zip'
    )
    expect(snippets.sha256).toBe(
      'echo "abc123  gtin-open-data.zip" | sha256sum -c -'
    )
    expect(snippets.pandas).toContain('compression="zip"')
    expect(snippets.duckdb).toContain('gtin-open-data.zip/gtin-open-data.csv')
    expect(snippets.r).toContain('read_csv("gtin-open-data.zip")')
  })

  it('builds a complete Dataset JSON-LD from the manifest values', () => {
    const jsonLd = buildDatasetJsonLd({
      name: 'GTIN',
      description: 'desc',
      pageUrl: 'https://nudger.fr/opendata/gtin',
      inLanguage: 'fr-FR',
      organizationName: 'Nudger',
      organizationUrl: 'https://nudger.fr',
      doi: '10.5281/zenodo.14035436',
      datasets: [
        {
          id: 'gtin',
          generatedAt: '2026-11-22T02:55:00Z',
          sizeBytes: 6_400_000_000,
          sha256: 'abc',
          downloadUrl: 'https://data.nudger.fr/gtin-open-data.zip',
        },
      ],
      columns: [{ name: 'gtin', description: 'Code-barres' }],
    })

    expect(jsonLd['@type']).toBe('Dataset')
    expect(jsonLd.dateModified).toBe('2026-11-22T02:55:00Z')
    expect(jsonLd.contentSize).toBe('6.4 GB')
    expect(jsonLd.identifier).toBe('https://doi.org/10.5281/zenodo.14035436')
    expect(jsonLd.license).toBe(
      'https://opendatacommons.org/licenses/odbl/1-0/'
    )
    expect(jsonLd.creator['@type']).toBe('Organization')
    expect(jsonLd.publisher['@type']).toBe('Organization')
    expect(jsonLd.distribution[0]?.contentUrl).toBe(
      'https://data.nudger.fr/gtin-open-data.zip'
    )
    expect(jsonLd.sameAs.length).toBeGreaterThan(2)
    expect(jsonLd.variableMeasured?.[0]?.name).toBe('gtin')
  })

  it('leaves out values that the manifest does not provide', () => {
    const jsonLd = buildDatasetJsonLd({
      name: 'x',
      description: 'y',
      pageUrl: 'https://nudger.fr/opendata',
      inLanguage: 'fr-FR',
      organizationName: 'Nudger',
      organizationUrl: 'https://nudger.fr',
      datasets: [{ id: 'gtin' }],
    })
    expect(jsonLd).not.toHaveProperty('dateModified')
    expect(jsonLd).not.toHaveProperty('contentSize')
    expect(jsonLd).not.toHaveProperty('identifier')
    expect(jsonLd).not.toHaveProperty('variableMeasured')
    expect(jsonLd.distribution).toEqual([])
  })
})
