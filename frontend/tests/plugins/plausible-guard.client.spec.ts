import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useRuntimeConfig } from '#imports'
import plugin from '~/plugins/plausible-guard.client'

const runSetup = () => (plugin as unknown as { setup: () => void }).setup()

const plausibleConfig = () =>
  useRuntimeConfig().public.plausible as { enabled?: boolean }

const setReferrer = (value: string) =>
  vi.spyOn(document, 'referrer', 'get').mockReturnValue(value)

describe('plausible-privacy-guard plugin', () => {
  beforeEach(() => {
    useRuntimeConfig().public.plausible = {}
    window.history.pushState({}, '', '/blog')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('disables Plausible on /contrib/ pages', () => {
    window.history.pushState({}, '', '/contrib/abc')
    runSetup()
    expect(plausibleConfig().enabled).toBe(false)
  })

  it('leaves Plausible enabled elsewhere', () => {
    runSetup()
    expect(plausibleConfig().enabled).not.toBe(false)
  })

  it.each([
    'http://127.0.0.1:4100/',
    'http://localhost:3000/x',
    'http://[::1]/',
    'http://app.localhost/',
    'http://127.1.2.3/',
    'http://0.0.0.0:8080/',
  ])('disables Plausible for local referrer %s', referrer => {
    setReferrer(referrer)
    runSetup()
    expect(plausibleConfig().enabled).toBe(false)
  })

  it.each([
    'https://www.google.com/',
    'https://nudger.fr/produit',
    'https://localhost.example.com/',
    '',
    'not a url',
  ])('keeps Plausible enabled for referrer "%s"', referrer => {
    setReferrer(referrer)
    runSetup()
    expect(plausibleConfig().enabled).not.toBe(false)
  })
})
