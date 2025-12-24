<script setup lang="ts">
import { computed, type CSSProperties, type MaybeRef } from 'vue'
import HeroSurface from '~/components/shared/hero/HeroSurface.vue'
import ParallaxWidget from '~/components/shared/ui/ParallaxWidget.vue'
import HeroEducationCard from '~/components/shared/ui/HeroEducationCard.vue'
import TextContent from '~/components/domains/content/TextContent.vue'
import type { PageHeaderProps, PageHeaderEmits } from './types'
import { useHeaderSeo } from './composables/useHeaderSeo'
import { useHeaderA11y } from './composables/useHeaderA11y'
import { useHeaderLayout } from './composables/useHeaderLayout'
import { useThemeAsset } from '~/composables/useThemedAsset'
import type { ThemeAssetKey } from '~/config/theme/assets'

/**
 * Unified Page Header Component
 *
 * Flexible, SEO-ready, accessible page header with support for:
 * - 3 variants: hero-fullscreen, hero-standard, section-header
 * - Multiple layouts: single-column, 2-columns, 3-columns
 * - Background types: gradient, image, parallax, surface-variant
 * - Structured data (JSON-LD), ARIA, i18n
 *
 * @see /docs/page-header-audit-report.md
 */

const props = withDefaults(defineProps<PageHeaderProps>(), {
  // Layout defaults
  layout: 'single-column',
  contentAlign: 'start',
  container: 'lg',
  maxWidth: undefined,

  // Content defaults
  eyebrow: undefined,
  subtitle: undefined,
  descriptionBlocId: undefined,
  descriptionHtml: undefined,

  // Background defaults
  background: 'surface-variant',
  surfaceVariant: 'aurora',
  backgroundImage: undefined,
  overlayOpacity: 0.65,
  backgroundColor: undefined,

  // Parallax defaults
  isParallax: false,
  parallaxLayers: () => [],
  parallaxAmount: 0.18,
  enableAplats: false,
  aplatSvg: '/images/home/parallax-aplats.svg',

  // Height defaults
  minHeight: null,
  paddingY: null,

  // CTA defaults
  primaryCta: undefined,
  secondaryCta: undefined,
  ctaGroupLabel: undefined,

  // Media defaults
  showMedia: false,
  mediaType: 'image',
  mediaImage: undefined,
  mediaImageAlt: undefined,
  heroCard: undefined,

  // SEO defaults
  headingLevel: 'h1',
  headingId: undefined,
  breadcrumbs: () => [],
  schemaType: 'WebPage',
  ogImage: undefined,

  // A11y defaults
  ariaLabel: undefined,
  ariaDescribedBy: undefined,

  // Animation defaults
  animate: false,
  animationType: 'fade',
  animateOnScroll: false,
})

const emit = defineEmits<PageHeaderEmits>()

// === Composables ===
const { headingLabelId, regionAttrs, ctaGroupAttrs } = useHeaderA11y({
  headingId: props.headingId,
  ariaLabel: props.ariaLabel,
  ariaDescribedBy: props.ariaDescribedBy,
  ctaGroupLabel: props.ctaGroupLabel,
})

useHeaderSeo({
  title: props.title,
  subtitle: props.subtitle,
  description: props.descriptionHtml,
  headingLevel: props.headingLevel,
  schemaType: props.schemaType,
  breadcrumbs: props.breadcrumbs,
  ogImage: props.ogImage,
})

const {
  containerClasses,
  containerStyles,
  vuetifyContainerProps,
  gridColumns,
  hasMediaColumn,
} = useHeaderLayout({
  variant: props.variant,
  layout: props.layout,
  contentAlign: props.contentAlign,
  container: props.container,
  maxWidth: props.maxWidth,
  minHeight: props.minHeight,
  paddingY: props.paddingY,
})

// === Computed ===
const HeadingTag = computed(() => props.headingLevel)

const showParallax = computed(
  () => props.background === 'parallax' && props.isParallax
)

const showImage = computed(
  () => props.background === 'image' && props.backgroundImage
)

const showGradient = computed(() => props.background === 'gradient')

const showSurfaceVariant = computed(
  () => props.background === 'surface-variant'
)

const backgroundImageAssetKeyRef = computed(
  () => props.backgroundImageAssetKey as ThemeAssetKey | undefined
)
const resolvedBackgroundAsset = useThemeAsset(
  backgroundImageAssetKeyRef as MaybeRef<ThemeAssetKey>
)

const backgroundImageSrc = computed(() => {
  if (props.backgroundImageAssetKey && resolvedBackgroundAsset.value) {
    return resolvedBackgroundAsset.value
  }

  if (typeof props.backgroundImage === 'string') {
    return props.backgroundImage
  }
  // TODO: theme-aware image selection
  return props.backgroundImage?.light || ''
})

const backgroundStyles = computed<CSSProperties>(() => {
  if (showGradient.value) {
    return {
      background: `linear-gradient(
        135deg,
        rgba(var(--v-theme-hero-gradient-start), 0.95),
        rgba(var(--v-theme-hero-gradient-end), 0.9)
      )`,
      color: 'rgb(var(--v-theme-hero-pill-on-dark))',
    }
  }

  if (props.backgroundColor) {
    return { backgroundColor: props.backgroundColor }
  }

  return {}
})

// === Event Handlers ===
const handlePrimaryCta = () => {
  emit('cta:primary')
}

const handleSecondaryCta = () => {
  emit('cta:secondary')
}

const handleIntersection = (
  _entries: unknown,
  _observer: unknown,
  isIntersecting: boolean
) => {
  emit('intersection', isIntersecting)
}
</script>

<template>
  <component
    :is="showSurfaceVariant ? HeroSurface : 'section'"
    v-intersect="animateOnScroll ? handleIntersection : undefined"
    :class="containerClasses"
    :style="[containerStyles, backgroundStyles]"
    v-bind="regionAttrs"
    :variant="showSurfaceVariant ? surfaceVariant : undefined"
  >
    <!-- Background: Parallax Widget -->
    <ParallaxWidget
      v-if="showParallax"
      :backgrounds="parallaxLayers"
      :parallax-amount="parallaxAmount"
      :overlay-opacity="overlayOpacity"
      :enable-aplats="enableAplats"
      :aplat-svg="aplatSvg"
      class="page-header__parallax"
    >
      <slot name="parallax-content">
        <!-- Content goes inside ParallaxWidget if parallax enabled -->
      </slot>
    </ParallaxWidget>

    <!-- Background: Image -->
    <div v-if="showImage" class="page-header__background" aria-hidden="true">
      <img
        :src="backgroundImageSrc"
        alt=""
        class="page-header__background-image"
        loading="eager"
        fetchpriority="high"
      />
      <div class="page-header__background-overlay" />
    </div>

    <!-- Background: Custom Slot -->
    <slot name="background" />

    <!-- Main Content Container -->
    <v-container v-bind="vuetifyContainerProps" class="page-header__container">
      <!-- Breadcrumbs (if provided) -->
      <v-breadcrumbs
        v-if="breadcrumbs && breadcrumbs.length"
        :items="breadcrumbs.map(b => ({ title: b.label, href: b.href }))"
        class="page-header__breadcrumbs"
        density="compact"
      />

      <v-row
        :class="{
          'page-header__row': true,
          'page-header__row--centered': contentAlign === 'center',
        }"
        align="center"
        justify="center"
      >
        <!-- Main Content Column -->
        <v-col
          :cols="12"
          :md="hasMediaColumn ? gridColumns.main : 12"
          class="page-header__content"
        >
          <!-- Slot: eyebrow (or default eyebrow) -->
          <slot name="eyebrow">
            <span v-if="eyebrow" class="page-header__eyebrow">
              {{ eyebrow }}
            </span>
          </slot>

          <!-- Slot: title (or default heading) -->
          <slot name="title">
            <component
              :is="HeadingTag"
              :id="headingLabelId"
              class="page-header__title"
            >
              {{ title }}
            </component>
          </slot>

          <!-- Slot: subtitle -->
          <slot name="subtitle">
            <p v-if="subtitle" class="page-header__subtitle">
              {{ subtitle }}
            </p>
          </slot>

          <!-- Slot: description (or CMS content) -->
          <slot name="description">
            <div
              v-if="descriptionBlocId || descriptionHtml"
              class="page-header__description"
            >
              <TextContent
                v-if="descriptionBlocId"
                :bloc-id="descriptionBlocId"
              />
              <!-- eslint-disable-next-line vue/no-v-html -->
              <div v-else-if="descriptionHtml" v-html="descriptionHtml" />
            </div>
          </slot>

          <!-- Slot: actions (or default CTAs) -->
          <slot name="actions">
            <div
              v-if="primaryCta || secondaryCta"
              class="page-header__actions"
              v-bind="ctaGroupAttrs"
            >
              <v-btn
                v-if="primaryCta"
                :href="primaryCta.href"
                :aria-label="primaryCta.ariaLabel"
                :color="primaryCta.color ?? 'primary'"
                :variant="primaryCta.variant ?? 'flat'"
                :target="primaryCta.target"
                :rel="primaryCta.rel"
                size="large"
                class="page-header__cta page-header__cta--primary"
                @click="handlePrimaryCta"
              >
                <template v-if="primaryCta.icon" #prepend>
                  <v-icon :icon="primaryCta.icon" aria-hidden="true" />
                </template>
                {{ primaryCta.label }}
              </v-btn>

              <v-btn
                v-if="secondaryCta"
                :href="secondaryCta.href"
                :aria-label="secondaryCta.ariaLabel"
                :color="secondaryCta.color ?? 'secondary'"
                :variant="secondaryCta.variant ?? 'outlined'"
                :target="secondaryCta.target"
                :rel="secondaryCta.rel"
                size="large"
                class="page-header__cta page-header__cta--secondary"
                @click="handleSecondaryCta"
              >
                <template v-if="secondaryCta.icon" #prepend>
                  <v-icon :icon="secondaryCta.icon" aria-hidden="true" />
                </template>
                {{ secondaryCta.label }}
              </v-btn>
            </div>
          </slot>

          <!-- Default Slot (custom content) -->
          <slot />
        </v-col>

        <!-- Media Column (if 2-columns or 3-columns layout) -->
        <v-col
          v-if="showMedia && hasMediaColumn"
          :cols="12"
          :md="gridColumns.media || gridColumns.right"
          class="page-header__media"
        >
          <!-- Slot: media (or default media types) -->
          <slot name="media">
            <!-- HeroEducationCard -->
            <HeroEducationCard
              v-if="mediaType === 'card' && heroCard"
              v-bind="heroCard"
            />

            <!-- Image -->
            <img
              v-else-if="mediaType === 'image' && mediaImage"
              :src="mediaImage"
              :alt="mediaImageAlt || ''"
              class="page-header__media-image"
            />

            <!-- Glow visual (from PartnersHero) -->
            <div
              v-else-if="mediaType === 'glow'"
              class="page-header__glow"
              aria-hidden="true"
            >
              <div class="page-header__glow-ring" />
              <div
                class="page-header__glow-ring page-header__glow-ring--secondary"
              />
            </div>
          </slot>
        </v-col>
      </v-row>
    </v-container>

    <!-- Slot: aplats (SVG decorations for parallax) -->
    <slot name="aplats" />
  </component>
</template>

<style scoped lang="sass">
// === Root Container ===
.page-header
  position: relative
  overflow: hidden
  width: 100%
  // Support custom background color from props
  background-color: v-bind(backgroundColor)

// === Variants ===
.page-header--hero-fullscreen
  min-height: 100dvh
  padding-block: clamp(2.5rem, 7vw, 4.75rem)

.page-header--hero-standard
  padding-block: clamp(3rem, 8vw, 5.5rem)

.page-header--section-header
  padding-block: clamp(2rem, 5vw, 3.5rem)

// === Background ===
.page-header__background
  position: absolute
  inset: 0
  z-index: 0
  pointer-events: none

.page-header__background-image
  position: absolute
  inset: 0
  width: 100%
  height: 100%
  object-fit: cover
  opacity: 0.98

.page-header__background-overlay
  position: absolute
  inset: 0
  background: radial-gradient(circle at 16% 24%, rgba(var(--v-theme-hero-gradient-start), 0.22), transparent 32%), radial-gradient(circle at 78% 12%, rgba(var(--v-theme-hero-gradient-end), 0.24), transparent 36%), linear-gradient(180deg, rgba(var(--v-theme-surface-default), 0.1) 0%, rgba(var(--v-theme-surface-default), 0.15) 35%, rgba(var(--v-theme-surface-default), 0.65) 100%)

// === Container ===
.page-header__container
  position: relative
  z-index: 1
  padding-inline: clamp(1.5rem, 5vw, 4rem)

// === Content ===
.page-header__content
  display: flex
  flex-direction: column
  gap: clamp(0.75rem, 2vw, 1.25rem)

.page-header__eyebrow
  align-self: flex-start
  padding: 0.375rem 0.75rem
  border-radius: 999px
  background: rgba(var(--v-theme-hero-overlay-strong), 0.12)
  font-size: 0.875rem
  font-weight: 600
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgba(var(--v-theme-hero-gradient-end), 0.9)

.page-header__title
  font-size: clamp(2.2rem, 5vw, 3.8rem)
  font-weight: 700
  line-height: 1.1
  margin: 0

.page-header__subtitle
  font-size: clamp(1rem, 2.4vw, 1.4rem)
  line-height: 1.4
  margin: 0
  opacity: 0.9

.page-header__description
  font-size: 1.05rem
  line-height: 1.6

// === Actions (CTA) ===
.page-header__actions
  display: flex
  flex-wrap: wrap
  gap: 0.75rem
  margin-top: 1rem

.page-header__cta
  font-weight: 600

// === Media ===
.page-header__media
  display: flex
  justify-content: center
  align-items: center

.page-header__media-image
  width: 100%
  max-width: 480px
  height: auto
  border-radius: 1rem

// Glow visual (from PartnersHero)
.page-header__glow
  position: relative
  width: min(320px, 100%)
  aspect-ratio: 1

.page-header__glow-ring
  position: absolute
  inset: 10%
  border-radius: 50%
  border: 1px solid rgba(var(--v-theme-hero-overlay-strong), 0.35)
  box-shadow: 0 0 60px rgba(var(--v-theme-accent-primary-highlight), 0.25)

.page-header__glow-ring--secondary
  inset: 20%
  border-color: rgba(var(--v-theme-accent-supporting), 0.3)
  box-shadow: 0 0 40px rgba(var(--v-theme-accent-supporting), 0.22)

// === Alignment ===
.page-header--align-center .page-header__content
  align-items: center
  text-align: center

.page-header--align-start .page-header__content
  align-items: flex-start
  text-align: left

// === Responsive ===
@media (max-width: 959px)
  .page-header--hero-fullscreen
    min-height: clamp(520px, 68dvh, 760px)
    padding-block: clamp(2rem, 10vw, 4rem)

  .page-header__media
    margin-top: 2rem

  .page-header__glow
    width: 240px
</style>
