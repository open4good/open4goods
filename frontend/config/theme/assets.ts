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

export const EVENT_PACK_NAMES = [
  'default',
  'sdg',
  'bastille-day',
  'hold',
] as const

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
    logo: 'placeholders/logo.svg',
    footerLogo: 'placeholders/logo-footer.svg',
    favicon: 'placeholders/favicon.svg',
    heroBackground: 'placeholders/hero-background.svg',
    illustration: 'placeholders/illustration.svg',
  },
  dark: {
    logo: 'placeholders/logo.svg',
    footerLogo: 'placeholders/logo-footer.svg',
    favicon: 'placeholders/favicon.svg',
    heroBackground: 'placeholders/hero-background.svg',
    illustration: 'placeholders/illustration.svg',
  },
  common: {
    logo: 'placeholders/asset-missing.svg',
    footerLogo: 'placeholders/asset-missing.svg',
    favicon: 'placeholders/asset-missing.svg',
    heroBackground: 'placeholders/asset-missing.svg',
    illustration: 'placeholders/asset-missing.svg',
  },
}

export const seasonalThemeAssets: SeasonalThemeAssets = {
  'bastille-day': {
    light: {
      heroBackground: 'bastille-day/hero-background.svg',
      illustration: 'bastille-day/illustration-fireworks.svg',
    },
    dark: {},
    common: {
      heroBackground: 'bastille-day/hero-background.svg',
      illustration: 'bastille-day/illustration-fireworks.svg',
    },
  },
  hold: {
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
      heroBackground: 'hero-background.webp',
      illustration: 'illustration-generic.svg',
    },
    common: {
      logo: 'logo.png',
      heroBackground: 'hero-background.svg',
      illustration: 'illustration-generic.svg',
    },
  },
}

export const eventParallaxPacks: Record<
  ThemeName | 'common',
  Partial<Record<EventPackName, ParallaxPackConfig>>
> = {
  light: {
    default: {
      essentials: ['parallax/parallax-placeholder.svg'],
      features: ['parallax/parallax-placeholder.svg'],
      blog: ['parallax/parallax-placeholder.svg'],
      objections: ['parallax/parallax-placeholder.svg'],
      cta: ['parallax/parallax-placeholder.svg'],
    },
    'bastille-day': {
      essentials: ['parallax/parallax-background-bastille-essentials.svg'],
      features: ['parallax/parallax-background-bastille-features.svg'],
      blog: ['parallax/parallax-background-bastille-blog.svg'],
      objections: ['parallax/parallax-background-bastille-objections.svg'],
      cta: ['parallax/parallax-background-bastille-cta.svg'],
    },
    sdg: {
      essentials: ['parallax/parallax-placeholder.svg'],
      features: ['parallax/parallax-placeholder.svg'],
      blog: ['parallax/parallax-placeholder.svg'],
      objections: ['parallax/parallax-placeholder.svg'],
      cta: ['parallax/parallax-placeholder.svg'],
    },
    hold: {
      essentials: ['parallax/hero-back--light.svg'],
      features: ['parallax/hero-mid--light.svg'],
      blog: ['parallax/hero-front--light.svg'],
      objections: ['parallax/hero-back--light.svg'],
      cta: ['parallax/hero-mid--light.svg'],
    },
  },
  dark: {
    default: {
      essentials: ['parallax/parallax-placeholder.svg'],
      features: ['parallax/parallax-placeholder.svg'],
      blog: ['parallax/parallax-placeholder.svg'],
      objections: ['parallax/parallax-placeholder.svg'],
      cta: ['parallax/parallax-placeholder.svg'],
    },
    'bastille-day': {
      essentials: ['parallax/parallax-background-bastille-essentials.svg'],
      features: ['parallax/parallax-background-bastille-features.svg'],
      blog: ['parallax/parallax-background-bastille-blog.svg'],
      objections: ['parallax/parallax-background-bastille-objections.svg'],
      cta: ['parallax/parallax-background-bastille-cta.svg'],
    },
    sdg: {
      essentials: ['parallax/parallax-placeholder.svg'],
      features: ['parallax/parallax-placeholder.svg'],
      blog: ['parallax/parallax-placeholder.svg'],
      objections: ['parallax/parallax-placeholder.svg'],
      cta: ['parallax/parallax-placeholder.svg'],
    },
    hold: {
      essentials: ['parallax/hero-back--dark.svg'],
      features: ['parallax/hero-mid--dark.svg'],
      blog: ['parallax/hero-front--dark.svg'],
      objections: ['parallax/hero-back--dark.svg'],
      cta: ['parallax/hero-mid--dark.svg'],
    },
  },
  common: {
    default: {
      essentials: ['parallax/parallax-placeholder.svg'],
      features: ['parallax/parallax-placeholder.svg'],
      blog: ['parallax/parallax-placeholder.svg'],
      objections: ['parallax/parallax-placeholder.svg'],
      cta: ['parallax/parallax-placeholder.svg'],
    },
    'bastille-day': {
      essentials: ['parallax/parallax-background-bastille-essentials.svg'],
      features: ['parallax/parallax-background-bastille-features.svg'],
      blog: ['parallax/parallax-background-bastille-blog.svg'],
      objections: ['parallax/parallax-background-bastille-objections.svg'],
      cta: ['parallax/parallax-background-bastille-cta.svg'],
    },
    sdg: {},
    hold: {
      essentials: ['parallax/missing-parallax.svg'],
      features: ['parallax/missing-parallax.svg'],
      blog: ['parallax/missing-parallax.svg'],
      objections: ['parallax/missing-parallax.svg'],
      cta: ['parallax/missing-parallax.svg'],
    },
  },
}
/** @deprecated Use {@link eventParallaxPacks} instead. */
export const parallaxPacks = eventParallaxPacks
