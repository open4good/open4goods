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

export const EVENT_PACK_NAMES = ['default', 'sdg', 'christmas'] as const

export type ParallaxSectionKey = (typeof PARALLAX_SECTION_KEYS)[number]
export type EventPackName = (typeof EVENT_PACK_NAMES)[number]

/** @deprecated Use {@link EVENT_PACK_NAMES} instead. */
export const PARALLAX_PACK_NAMES = EVENT_PACK_NAMES
/** @deprecated Use {@link EventPackName} instead. */
export type ParallaxPackName = EventPackName

export type SeasonalThemeAssets = Partial<
  Record<EventPackName, Partial<Record<ThemeName | 'common', ThemeAssetConfig>>>
>

export const DEFAULT_EVENT_PACK: EventPackName = 'default'
/** @deprecated Use {@link DEFAULT_EVENT_PACK} instead. */
export const DEFAULT_PARALLAX_PACK: ParallaxPackName = DEFAULT_EVENT_PACK

export type ParallaxLayerConfig = {
  src: string
  speed?: number
  blendMode?: string
}

export type ParallaxLayerSource = string | ParallaxLayerConfig

export type ParallaxPackConfig = Partial<
  Record<ParallaxSectionKey, ParallaxLayerSource[]>
>

export const themeAssets: Record<ThemeName | 'common', ThemeAssetConfig> = {
  light: {
    logo: 'logo-new.png',
    footerLogo: 'logo-footer.svg',
    favicon: 'favicon.svg',
    heroBackground: 'hero-background.webp',
    illustration: 'illustration-generic.svg',
  },
  dark: {
    logo: 'logo-new.png',
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

export const seasonalThemeAssets: SeasonalThemeAssets = {
  christmas: {
    light: {
      heroBackground: 'hero-background.svg',
      illustration: 'illustration-generic.svg',
    },
    dark: {
      heroBackground: 'hero-background.svg',
      illustration: 'illustration-generic.svg',
    },
    common: {},
  },
}

export const eventParallaxPacks: Record<
  ThemeName | 'common',
  Partial<Record<EventPackName, ParallaxPackConfig>>
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
      essentials: ['parallax/parallax-background-christmas-essentials.svg'],
      features: ['parallax/parallax-background-christmas-features.svg'],
      blog: ['parallax/parallax-background-christmas-blog.svg'],
      objections: ['parallax/parallax-background-christmas-objections.svg'],
      cta: ['parallax/parallax-background-christmas-cta.svg'],
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
      essentials: ['parallax/parallax-background-christmas-essentials.svg'],
      features: ['parallax/parallax-background-christmas-features.svg'],
      blog: ['parallax/parallax-background-christmas-blog.svg'],
      objections: ['parallax/parallax-background-christmas-objections.svg'],
      cta: ['parallax/parallax-background-christmas-cta.svg'],
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
/** @deprecated Use {@link eventParallaxPacks} instead. */
export const parallaxPacks = eventParallaxPacks
