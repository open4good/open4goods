import type { ThemeName } from '~~/shared/constants/theme'

export const THEME_ASSETS_FALLBACK: ThemeName = 'light'

export const THEME_ASSET_KEYS = ['logo', 'footerLogo', 'favicon', 'heroBackground', 'illustration'] as const

export type ThemeAssetKey = (typeof THEME_ASSET_KEYS)[number]

export type ThemeAssetConfig = Partial<Record<ThemeAssetKey, string>>

export const themeAssets: Record<ThemeName | 'common', ThemeAssetConfig> = {
  light: {
    logo: 'logo.png',
    footerLogo: 'logo-footer.svg',
    favicon: 'favicon.svg',
    heroBackground: 'hero-background.svg',
    illustration: 'illustration-generic.svg',
  },
  dark: {
    logo: 'logo.png',
    footerLogo: 'logo-footer.svg',
    favicon: 'favicon.svg',
    heroBackground: 'hero-background.svg',
    illustration: 'illustration-generic.svg',
  },
  common: {
    heroBackground: 'hero-background.svg',
    illustration: 'illustration-generic.svg',
  },
}
