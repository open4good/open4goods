import type { ThemeName } from '~~/shared/constants/theme'

export const THEME_ASSETS_FALLBACK: ThemeName = 'light'

export const THEME_ASSET_KEYS = [
  'logo',
  'footerLogo',
  'favicon',
  'heroBackground',
  'illustration',
] as const

export type ThemeAssetKey = (typeof THEME_ASSET_KEYS)[number]

export type ThemeAssetConfig = Partial<Record<ThemeAssetKey, string>>

export const PARALLAX_SECTION_KEYS = [
  'essentials',
  'features',
  'blog',
  'objections',
  'cta',
] as const

export const PARALLAX_PACK_NAMES = ['default', 'sdg', 'christmas'] as const

export type ParallaxSectionKey = (typeof PARALLAX_SECTION_KEYS)[number]
export type ParallaxPackName = (typeof PARALLAX_PACK_NAMES)[number]

export const DEFAULT_PARALLAX_PACK: ParallaxPackName = 'default'

export type ParallaxPackConfig = Partial<Record<ParallaxSectionKey, string[]>>

export const themeAssets: Record<ThemeName | 'common', ThemeAssetConfig> = {
  light: {
    logo: 'logo.png',
    footerLogo: 'logo-footer.svg',
    favicon: 'favicon.svg',
    heroBackground: 'hero-background.webp',
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

export const parallaxPacks: Record<
  ThemeName | 'common',
  Partial<Record<ParallaxPackName, ParallaxPackConfig>>
> = {
  light: {
    default: {
      essentials: ['parallax/parallax-background-1.svg'],
      features: ['parallax/parallax-background-2.svg'],
      blog: ['parallax/parallax-background-3.svg'],
      objections: ['parallax/parallax-background-1.svg'],
      cta: ['parallax/parallax-background-2.svg'],
    },
    sdg: {
      essentials: ['parallax/parallax-background-sdg-essentials.svg'],
      features: ['parallax/parallax-background-sdg-features.svg'],
      blog: ['parallax/parallax-background-sdg-blog.svg'],
      objections: ['parallax/parallax-background-sdg-objections.svg'],
      cta: ['parallax/parallax-background-sdg-cta.svg'],
    },
    christmas: {
      essentials: ['parallax/parallax-background-bubbles-1.svg'],
      features: ['parallax/parallax-background-bubbles-2.svg'],
      blog: ['parallax/parallax-background-bubbles-3.svg'],
      objections: ['parallax/parallax-background-bubbles-1.svg'],
      cta: ['parallax/parallax-background-bubbles-2.svg'],
    },
  },
  dark: {
    default: {
      essentials: ['parallax/parallax-background-1.svg'],
      features: ['parallax/parallax-background-2.svg'],
      blog: ['parallax/parallax-background-3.svg'],
      objections: ['parallax/parallax-background-1.svg'],
      cta: ['parallax/parallax-background-2.svg'],
    },
    sdg: {
      essentials: ['parallax/parallax-background-sdg-essentials.svg'],
      features: ['parallax/parallax-background-sdg-features.svg'],
      blog: ['parallax/parallax-background-sdg-blog.svg'],
      objections: ['parallax/parallax-background-sdg-objections.svg'],
      cta: ['parallax/parallax-background-sdg-cta.svg'],
    },
    christmas: {
      essentials: ['parallax/parallax-background-bubbles-1.svg'],
      features: ['parallax/parallax-background-bubbles-2.svg'],
      blog: ['parallax/parallax-background-bubbles-3.svg'],
      objections: ['parallax/parallax-background-bubbles-1.svg'],
      cta: ['parallax/parallax-background-bubbles-2.svg'],
    },
  },
  common: {
    default: {
      essentials: ['parallax/parallax-background-1.svg'],
      features: ['parallax/parallax-background-2.svg'],
      blog: ['parallax/parallax-background-3.svg'],
      objections: ['parallax/parallax-background-1.svg'],
      cta: ['parallax/parallax-background-2.svg'],
    },
    sdg: {},
    christmas: {},
  },
}
