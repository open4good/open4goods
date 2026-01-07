/**
 * Types for unified PageHeader component
 *
 * @fileoverview Type definitions for the PageHeader component architecture
 * @module components/shared/header
 */

/**
 * Page header variant types
 */
export type PageHeaderVariant =
  | 'hero-fullscreen'
  | 'hero-standard'
  | 'section-header'

/**
 * Layout configuration for header content
 */
export type PageHeaderLayout = 'single-column' | '2-columns' | '3-columns'

/**
 * Content alignment options
 */
export type PageHeaderContentAlign = 'start' | 'center'

/**
 * Container size options (Vuetify)
 */
export type PageHeaderContainer = 'fluid' | 'lg' | 'xl' | 'xxl' | 'semi-fluid'

/**
 * Background type options
 */
export type PageHeaderBackground =
  | 'gradient'
  | 'image'
  | 'parallax'
  | 'solid'
  | 'surface-variant'

/**
 * HeroSurface variant types (from existing HeroSurface.vue)
 */
export type HeroSurfaceVariant =
  | 'aurora'
  | 'halo'
  | 'prism'
  | 'pulse'
  | 'mesh'
  | 'orbit'

/**
 * Heading level options for SEO
 */
export type PageHeaderHeadingLevel = 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6'

/**
 * Schema.org types for structured data
 */
export type PageHeaderSchemaType =
  | 'WebPage'
  | 'Article'
  | 'AboutPage'
  | 'ContactPage'
  | 'FAQPage'

/**
 * Media type for visual content
 */
export type PageHeaderMediaType = 'image' | 'card' | 'glow' | 'custom'

/**
 * Animation type options
 */
export type PageHeaderAnimationType = 'fade' | 'slide' | 'scale' | 'none'

/**
 * Call-to-action button configuration
 */
export interface PageHeaderCta {
  /** Button label text */
  label: string
  /** Link href */
  href: string
  /** Accessible label for screen readers */
  ariaLabel: string
  /** Material Design Icon name */
  icon?: string
  /** Button color (Vuetify color name) */
  color?: string
  /** Button variant */
  variant?: 'flat' | 'outlined' | 'tonal' | 'text' | 'plain'
  /** Link target attribute */
  target?: string
  /** Link rel attribute */
  rel?: string
}

/**
 * Breadcrumb item for navigation
 */
export interface PageHeaderBreadcrumbItem {
  /** Breadcrumb label */
  label: string
  /** Optional link (if not current page) */
  href?: string
}

/**
 * Hero education card item (from HeroEducationCard.vue)
 */
export interface HeroEducationCardItem {
  /** Icon name */
  icon?: string
  /** Item text */
  text: string
}

/**
 * Hero education card configuration
 */
export interface HeroEducationCardProps {
  /** Card icon */
  icon: string
  /** Card title */
  title: string
  /** HTML body content */
  bodyHtml?: string
  /** List of items */
  items?: HeroEducationCardItem[]
}

/**
 * Parallax layer configuration (from ParallaxWidget.vue)
 */
export interface ParallaxLayerConfig {
  /** Image source URL */
  src: string
  /** Parallax speed multiplier */
  speed?: number
  /** CSS blend mode */
  blendMode?: string
}

/**
 * Main PageHeader component props
 */
export interface PageHeaderProps {
  // === Variante & Layout ===
  /** Header variant type */
  variant?: PageHeaderVariant
  /** Content layout columns */
  layout?: PageHeaderLayout
  /** Content horizontal alignment */
  contentAlign?: PageHeaderContentAlign

  // === Container ===
  /** Container size */
  container?: PageHeaderContainer
  /** Custom max-width CSS value */
  maxWidth?: string

  // === Contenu Textuel ===
  /** Optional eyebrow/kicker text */
  eyebrow?: string
  /** Main title (required) */
  title: string
  /** Optional subtitle */
  subtitle?: string
  /** CMS bloc ID for description content */
  descriptionBlocId?: string
  /** Fallback HTML description */
  descriptionHtml?: string

  // === Background & Style ===
  /** Background type */
  background?: PageHeaderBackground
  /** HeroSurface variant (if background='surface-variant') */
  surfaceVariant?: HeroSurfaceVariant
  /** Background image source */
  backgroundImage?: string | { light: string; dark: string }
  /** Background image i18n asset key (e.g. 'heroBackground') */
  backgroundImageAssetKey?: string
  /** Overlay opacity (0-1) */
  overlayOpacity?: number
  /** Custom background color */
  backgroundColor?: string

  // === Parallax (si background='parallax') ===
  /** Enable parallax effect */
  isParallax?: boolean
  /** Parallax background layers */
  parallaxLayers?: ParallaxLayerConfig[]
  /** Parallax movement amount */
  parallaxAmount?: number
  /** Enable decorative SVG aplats */
  enableAplats?: boolean
  /** Custom aplat SVG source */
  aplatSvg?: string

  // === Hauteur ===
  /** Minimum height CSS value */
  minHeight?: string | null
  /** Vertical padding CSS value */
  paddingY?: string | null

  // === Actions (CTA) ===
  /** Primary call-to-action button */
  primaryCta?: PageHeaderCta
  /** Secondary call-to-action button */
  secondaryCta?: PageHeaderCta
  /** Accessible label for CTA group */
  ctaGroupLabel?: string

  // === Media / Visual ===
  /** Show media/visual column */
  showMedia?: boolean
  /** Media type to display */
  mediaType?: PageHeaderMediaType
  /** Media image source */
  mediaImage?: string
  /** Media image alt text (i18n key or string) */
  mediaImageAlt?: string
  /** Hero education card configuration */
  heroCard?: HeroEducationCardProps

  // === SEO ===
  /** Heading HTML tag level */
  headingLevel?: PageHeaderHeadingLevel
  /** Custom heading element ID */
  headingId?: string
  /** Breadcrumb navigation items */
  breadcrumbs?: PageHeaderBreadcrumbItem[]
  /** Schema.org type for structured data */
  schemaType?: PageHeaderSchemaType
  /** Open Graph image URL */
  ogImage?: string

  // === Accessibilité ===
  /** Accessible region label */
  ariaLabel?: string
  /** ID of element describing this region */
  ariaDescribedBy?: string

  // === Animations ===
  /** Enable entrance animations */
  animate?: boolean
  /** Animation type */
  animationType?: PageHeaderAnimationType
  /** Animate on scroll intersection */
  animateOnScroll?: boolean
}

/**
 * PageHeader component emits
 */
export interface PageHeaderEmits {
  /** Primary CTA button clicked */
  'cta:primary': []
  /** Secondary CTA button clicked */
  'cta:secondary': []
  /** Intersection observer triggered */
  intersection: [isIntersecting: boolean]
}
