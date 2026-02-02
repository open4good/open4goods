/**
 * Configuration des assets thématiques (light/dark)
 *
 * Ce fichier définit :
 * - Les assets de base par thème (logo, favicon, etc.)
 *
 * Pour les variations saisonnières, utilisez des overrides de thèmes
 * plutôt que des fichiers i18n.
 */

import type { ThemeName } from '~~/shared/constants/theme'

// ----------------------------------------------------------------------------
// Types pour les assets thématiques
// ----------------------------------------------------------------------------

export const THEME_ASSETS_FALLBACK: ThemeName = 'light'

export const THEME_ASSET_KEYS = [
  'logo',
  'footerLogo',
  'favicon',
  'heroBackground',
  'illustration',
  'problemImage',
  'solutionImage',
  'productBackground',
  'contactBackground',
  'blogBackground',
  'categoriesBackground',
  'openSourceBackground',
  'openDataBackground',
  'teamBackground',
  'impactScoreBackground',
  'partnersBackground',
  'compareBackground',
  'searchBackground',
  'feedbackBackground',
  'promptBackground',
  'releasesBackground',
  'parallaxAplat',
] as const

export type ThemeAssetKey = (typeof THEME_ASSET_KEYS)[number]
export type ThemeAssetConfig = Partial<Record<ThemeAssetKey, string>>

// ----------------------------------------------------------------------------
// Types pour les sections parallax
// ----------------------------------------------------------------------------

export const PARALLAX_SECTION_KEYS = [
  'essentials',
  'features',
  'blog',
  'objections',
  'cta',
] as const

export type ParallaxSectionKey = (typeof PARALLAX_SECTION_KEYS)[number]

export type ParallaxLayerConfig = {
  src: string
  speed?: number
  blendMode?: string
}

export type ParallaxLayerSource = string | ParallaxLayerConfig

export type ParallaxPackConfig = Partial<
  Record<ParallaxSectionKey, ParallaxLayerSource[]>
>

// ----------------------------------------------------------------------------
// Assets de base par thème
// ----------------------------------------------------------------------------

export const themeAssets: Record<ThemeName | 'common', ThemeAssetConfig> = {
  light: {
    logo: 'logo.svg',
    footerLogo: 'placeholders/logo-footer.svg',
    favicon: 'placeholders/favicon.svg',
    heroBackground: 'hero-background.webp',
    illustration: 'placeholders/illustration.svg',
    productBackground: 'product-background.svg',
    contactBackground: 'contact-background.svg',
    blogBackground: 'blog-background.svg',
    categoriesBackground: 'categories-background.svg',
    openSourceBackground: 'open-source-background.svg',
    openDataBackground: 'open-data-background.svg',
    teamBackground: 'backgrounds/team.svg',
    impactScoreBackground: 'backgrounds/impact-score.svg',
    partnersBackground: 'backgrounds/partners.svg',
    compareBackground: 'backgrounds/compare.svg',
    searchBackground: 'backgrounds/search.svg',
    feedbackBackground: 'backgrounds/feedback.svg',
    promptBackground: 'backgrounds/prompt.svg',
    releasesBackground: 'backgrounds/releases.svg',
    parallaxAplat: 'parallax/parallax-aplats.svg',
  },
  dark: {
    logo: 'logo.svg',
    footerLogo: 'placeholders/logo-footer.svg',
    favicon: 'placeholders/favicon.svg',
    heroBackground: 'hero-background.svg',
    illustration: 'placeholders/illustration.svg',
    productBackground: 'product-background.svg',
    contactBackground: 'contact-background.svg',
    blogBackground: 'blog-background.svg',
    categoriesBackground: 'categories-background.svg',
    openSourceBackground: 'open-source-background.svg',
    openDataBackground: 'open-data-background.svg',
    teamBackground: 'backgrounds/team.svg',
    impactScoreBackground: 'backgrounds/impact-score.svg',
    partnersBackground: 'backgrounds/partners.svg',
    compareBackground: 'backgrounds/compare.svg',
    searchBackground: 'backgrounds/search.svg',
    feedbackBackground: 'backgrounds/feedback.svg',
    promptBackground: 'backgrounds/prompt.svg',
    releasesBackground: 'backgrounds/releases.svg',
    parallaxAplat: 'parallax/parallax-aplats.svg',
  },
  common: {
    logo: 'placeholders/asset-missing.svg',
    footerLogo: 'placeholders/asset-missing.svg',
    favicon: 'placeholders/asset-missing.svg',
    heroBackground: 'placeholders/asset-missing.svg',
    illustration: 'placeholders/asset-missing.svg',
    productBackground: 'placeholders/asset-missing.svg',
    contactBackground: 'placeholders/asset-missing.svg',
    blogBackground: 'placeholders/asset-missing.svg',
    categoriesBackground: 'placeholders/asset-missing.svg',
    openSourceBackground: 'placeholders/asset-missing.svg',
    openDataBackground: 'placeholders/asset-missing.svg',
    teamBackground: 'placeholders/generated/team.svg',
    impactScoreBackground: 'placeholders/generated/impact-score.svg',
    partnersBackground: 'placeholders/generated/partners.svg',
    compareBackground: 'placeholders/generated/compare.svg',
    searchBackground: 'placeholders/generated/search.svg',
    feedbackBackground: 'placeholders/generated/feedback.svg',
    promptBackground: 'placeholders/generated/prompt.svg',
    releasesBackground: 'placeholders/generated/releases.svg',
    parallaxAplat: 'parallax/parallax-aplats.svg',
  },
}
