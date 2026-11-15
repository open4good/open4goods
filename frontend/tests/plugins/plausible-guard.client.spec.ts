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
    // happy-dom expose webdriver=true : on simule un navigateur ordinaire.
    vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 Firefox/130.0' })
    window.history.pushState({}, '', '/blog')
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
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

  describe('automated browsers', () => {
    const tencentUa =
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36'
    const setNav = (props: Record<string, unknown>) => {
      vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0', ...props })
    }

    it('disables Plausible for a desktop Chrome UA without userAgentData', () => {
      vi.stubGlobal('isSecureContext', true)
      setNav({ userAgent: tencentUa, userAgentData: undefined })
      runSetup()
      expect(plausibleConfig().enabled).toBe(false)
    })

    it('disables Plausible when webdriver is true', () => {
      setNav({ webdriver: true })
      runSetup()
      expect(plausibleConfig().enabled).toBe(false)
    })

    it('keeps Plausible for a real Chrome 142', () => {
      vi.stubGlobal('isSecureContext', true)
      setNav({
        userAgent: tencentUa.replace('133', '142'),
        userAgentData: { brands: [{ brand: 'Chromium', version: '142' }] },
      })
      runSetup()
      expect(plausibleConfig().enabled).not.toBe(false)
    })

    it('keeps Plausible in an insecure context', () => {
      vi.stubGlobal('isSecureContext', false)
      setNav({ userAgent: tencentUa, userAgentData: undefined })
      runSetup()
      expect(plausibleConfig().enabled).not.toBe(false)
    })
  })
})
