import { describe, expect, it } from 'vitest'

import { resolveActiveEventPack, resolveEventPackName } from './seasons'

describe('resolveActiveEventPack', () => {
  it('returns default pack when no window matches', () => {
    const date = new Date(Date.UTC(2024, 6, 1))

    expect(resolveActiveEventPack(date)).toBe('default')
  })

  it('prefers SDG campaign window in spring', () => {
    const date = new Date(Date.UTC(2024, 3, 20))

    expect(resolveActiveEventPack(date)).toBe('sdg')
  })

  it('activates christmas pack within the festive window', () => {
    const date = new Date(Date.UTC(2024, 11, 15))

    expect(resolveActiveEventPack(date)).toBe('christmas')
  })

  it('falls back to winter pack before christmas window', () => {
    const date = new Date(Date.UTC(2024, 10, 30))

    expect(resolveActiveEventPack(date)).toBe('default')
  })

  it('keeps winter pack active after the new year', () => {
    const date = new Date(Date.UTC(2025, 0, 10))

    expect(resolveActiveEventPack(date)).toBe('default')
  })

  it('accepts an explicit pack name from query params', () => {
    expect(resolveEventPackName('christmas')).toBe('christmas')
    expect(resolveEventPackName(['sdg'])).toBe('sdg')
    expect(resolveEventPackName('unknown')).toBeUndefined()
  })
})
