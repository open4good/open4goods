import { describe, expect, it } from 'vitest'

import {
  THEME_ASSETS_FALLBACK,
  seasonalThemeAssets,
} from '~~/config/theme/assets'
import {
  resolveAssetPathForTheme,
  resolveThemedAssetUrlFromIndex,
} from './useThemedAsset'

describe('useThemedAsset utilities', () => {
  const assetIndex = {
    'light/logo-new.png': '/_nuxt/light-logo-new.png',
    'common/hero-background.svg': '/_nuxt/common-hero.svg',
    'light/christmas/hero-background.svg': '/_nuxt/light-christmas-hero.svg',
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

  it('prioritises seasonal overrides when available, then falls back', () => {
    seasonalThemeAssets.christmas = {
      ...seasonalThemeAssets.christmas,
      light: {
        heroBackground: 'hero-background.svg',
      },
    }

    const resolved = resolveThemedAssetUrlFromIndex(
      resolveAssetPathForTheme('heroBackground', 'light', 'christmas'),
      'light',
      assetIndex,
      THEME_ASSETS_FALLBACK,
      'christmas'
    )

    expect(resolved).toBe('/_nuxt/light-christmas-hero.svg')
  })

  it('returns multiple path candidates ordered by fallback', () => {
    expect(resolveAssetPathForTheme('logo', 'light')).toEqual(['logo-new.png'])
    expect(resolveAssetPathForTheme('heroBackground', 'dark')).toEqual([
      'hero-background.svg',
      'hero-background.webp',
    ])
  })
})
