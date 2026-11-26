import { describe, expect, it } from 'vitest'
import {
  buildWebVitalProps,
  DEFAULT_WEB_VITALS_SAMPLE_RATE,
  isWebVitalsSampled,
  normaliseSampleRate,
  resolvePageTemplate,
} from '~/utils/web-vitals'

describe('resolvePageTemplate', () => {
  it.each([
    ['/televiseurs/8806097084532-tv-samsung-tq77s90fae-2025', 'fiche'],
    ['/8806097084532-tv-samsung', 'fiche'],
    ['/televiseurs', 'categorie'],
    ['/televiseurs/', 'categorie'],
    ['/televiseurs/televiseur-55-pouces', 'sous-categorie'],
    ['/blog/quelle-taille-de-tv-choisir-pour-son-salon', 'blog'],
    ['/guides/acheter-un-lave-linge', 'guide'],
    ['/opendata/gtin', 'opendata'],
    ['/', 'autre'],
    ['/a/b/c', 'autre'],
  ])('%s -> %s', (path, expected) => {
    expect(resolvePageTemplate(path)).toBe(expected)
  })
})

describe('sampling', () => {
  it('falls back to the default rate on invalid input', () => {
    expect(normaliseSampleRate('abc')).toBe(DEFAULT_WEB_VITALS_SAMPLE_RATE)
    expect(normaliseSampleRate('2')).toBe(DEFAULT_WEB_VITALS_SAMPLE_RATE)
    expect(normaliseSampleRate(undefined)).toBe(DEFAULT_WEB_VITALS_SAMPLE_RATE)
    expect(normaliseSampleRate('0.25')).toBe(0.25)
    expect(normaliseSampleRate('0')).toBe(0)
  })

  it('samples strictly below the rate', () => {
    expect(isWebVitalsSampled(0.1, 0.05)).toBe(true)
    expect(isWebVitalsSampled(0.1, 0.1)).toBe(false)
    expect(isWebVitalsSampled(0, 0)).toBe(false)
  })
})

describe('buildWebVitalProps', () => {
  it('rounds milliseconds and scales CLS to an integer', () => {
    expect(
      buildWebVitalProps(
        { name: 'LCP', value: 2400.6, rating: 'good' },
        'fiche'
      )
    ).toEqual({ metric: 'LCP', template: 'fiche', rating: 'good', value: 2401 })
    expect(
      buildWebVitalProps({ name: 'CLS', value: 0.0834, rating: 'good' }, 'blog')
    ).toEqual({ metric: 'CLS', template: 'blog', rating: 'good', value: 83 })
  })
})
