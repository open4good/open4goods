import { beforeEach, describe, expect, it } from 'vitest'
import { useRuntimeConfig } from '#imports'
import plugin from '~/plugins/plausible-guard.client'

const runSetup = () => (plugin as unknown as { setup: () => void }).setup()

const plausibleConfig = () =>
  useRuntimeConfig().public.plausible as { enabled?: boolean }

describe('plausible-privacy-guard plugin', () => {
  beforeEach(() => {
    useRuntimeConfig().public.plausible = {}
  })

  it('disables Plausible on /contrib/ pages', () => {
    window.history.pushState({}, '', '/contrib/abc')
    runSetup()
    expect(plausibleConfig().enabled).toBe(false)
  })

  it('leaves Plausible enabled elsewhere', () => {
    window.history.pushState({}, '', '/blog')
    runSetup()
    expect(plausibleConfig().enabled).not.toBe(false)
  })
})
