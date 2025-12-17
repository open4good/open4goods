import { describe, expect, it } from 'vitest'

import { THEME_ASSETS_FALLBACK } from '~~/config/theme/assets'
import {
  resolveAssetPathForTheme,
  resolveThemedAssetUrlFromIndex,
} from './useThemedAsset'

describe('useThemedAsset utilities', () => {
  const assetIndex = {
    'light/logo.png': '/_nuxt/light-logo.png',
    'common/hero-background.svg': '/_nuxt/common-hero.svg',
  }

  it('returns a theme-specific asset when present', () => {
    const resolved = resolveThemedAssetUrlFromIndex(
      'logo.png',
      'light',
      assetIndex,
      THEME_ASSETS_FALLBACK
    )

    expect(resolved).toBe('/_nuxt/light-logo.png')
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
      'logo.png',
      'dark',
      assetIndex,
      THEME_ASSETS_FALLBACK
    )

    expect(resolved).toBe('/_nuxt/light-logo.png')
  })

  it('resolves mapped asset paths for themes', () => {
    expect(resolveAssetPathForTheme('logo', 'light')).toBe('logo.png')
    expect(resolveAssetPathForTheme('heroBackground', 'dark')).toBe(
      'hero-background.svg'
    )
  })
})
