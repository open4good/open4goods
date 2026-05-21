import { describe, expect, it, vi } from 'vitest'

import { THEME_ASSETS_FALLBACK } from '~~/config/theme/assets'
import {
  resolveThemedAssetLoaderFromIndex,
  resolveThemedAssetUrlFromIndex,
} from './useThemedAsset'

describe('useThemedAsset utilities', () => {
  const assetIndex = {
    'light/logo-new.png': '/_nuxt/light-logo-new.png',
    'common/hero-background.svg': '/_nuxt/common-hero.svg',
    'light/hero-background.svg': '/_nuxt/light-hero.svg',
  }

  it('returns a theme-specific asset when present', () => {
    const resolved = resolveThemedAssetUrlFromIndex(
      'logo-new.png',
      'light',
      assetIndex,
      THEME_ASSETS_FALLBACK
    )

    expect(resolved).toBe('/_nuxt/light-logo-new.png')
  })

  it('falls back to common assets when theme-specific files are missing', () => {
    const resolved = resolveThemedAssetUrlFromIndex(
      'hero-background.svg',
      'dark',
      assetIndex,
      THEME_ASSETS_FALLBACK
    )

    expect(resolved).toBe('/_nuxt/common-hero.svg')
  })

  it('falls back to the configured default theme when nothing else matches', () => {
    const resolved = resolveThemedAssetUrlFromIndex(
      'logo-new.png',
      'dark',
      assetIndex,
      THEME_ASSETS_FALLBACK
    )

    expect(resolved).toBe('/_nuxt/light-logo-new.png')
  })

  it('resolves a lazy asset loader without loading unrelated candidates', async () => {
    const commonLoader = vi.fn(async () => '/_nuxt/common-hero.svg')
    const lightLoader = vi.fn(async () => '/_nuxt/light-hero.svg')
    const loader = resolveThemedAssetLoaderFromIndex(
      'hero-background.svg',
      'dark',
      {
        'common/hero-background.svg': commonLoader,
        'light/hero-background.svg': lightLoader,
      },
      THEME_ASSETS_FALLBACK
    )

    expect(loader).toBe(commonLoader)
    expect(lightLoader).not.toHaveBeenCalled()
    expect(await loader?.()).toBe('/_nuxt/common-hero.svg')
    expect(commonLoader).toHaveBeenCalledTimes(1)
  })
})
