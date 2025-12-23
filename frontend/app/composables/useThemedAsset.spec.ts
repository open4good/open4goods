import { describe, expect, it } from 'vitest'

import { THEME_ASSETS_FALLBACK } from '~~/config/theme/assets'
import { resolveThemedAssetUrlFromIndex } from './useThemedAsset'

describe('useThemedAsset utilities', () => {
  const assetIndex = {
    'light/logo-new.png': '/_nuxt/light-logo-new.png',
    'common/hero-background.svg': '/_nuxt/common-hero.svg',
    'light/hold/hero-background.svg': '/_nuxt/light-hold-hero.svg',
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

  it('prioritises seasonal pack assets when pack is provided', () => {
    // Should resolve to light/hold/hero-background.svg
    const resolved = resolveThemedAssetUrlFromIndex(
      'hero-background.svg',
      'light',
      assetIndex,
      THEME_ASSETS_FALLBACK,
      'hold'
    )

    expect(resolved).toBe('/_nuxt/light-hold-hero.svg')
  })

<<<<<<< HEAD
  it('falls back to theme assets if seasonal pack asset is missing', () => {
    // 'hold' pack doesn't have logo-new.png in this mock index (only light/logo-new.png exists)
    const resolved = resolveThemedAssetUrlFromIndex(
      'logo-new.png',
      'light',
      assetIndex,
      THEME_ASSETS_FALLBACK,
      'hold'
    )

    expect(resolved).toBe('/_nuxt/light-logo-new.png')
  })
=======

>>>>>>> branch 'main' of https://github.com/open4good/open4goods.git
})
