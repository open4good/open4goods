<template>
  <div class="releases-page">
    <v-container class="releases-page__hero" max-width="lg">
      <v-row>
        <v-col cols="12" md="7" class="d-flex flex-column gap-4">
          <p class="releases-page__eyebrow">
            {{ t('releases.hero.eyebrow') }}
          </p>
          <div class="d-flex align-center gap-3 flex-wrap">
            <h1 class="releases-page__title">{{ t('releases.hero.title') }}</h1>
            <LatestReleaseBadge />
          </div>
          <p class="releases-page__subtitle">
            {{ t('releases.hero.subtitle') }}
          </p>
          <p class="releases-page__description">
            {{ t('releases.hero.description') }}
          </p>
          <div class="d-flex gap-3 flex-wrap">
            <v-btn
              color="primary"
              variant="flat"
              size="large"
              :to="t('releases.hero.ctaLink')"
            >
              {{ t('releases.hero.cta') }}
            </v-btn>
            <v-btn
              color="primary"
              variant="tonal"
              size="large"
              :to="localePath('contact')"
            >
              {{ t('releases.hero.secondaryCta') }}
            </v-btn>
          </div>
        </v-col>
        <v-col cols="12" md="5" class="d-flex justify-center align-center">
          <v-sheet class="releases-page__card" rounded="xl" elevation="4">
            <div class="releases-page__card-header">
              <v-icon icon="mdi-text-box-multiple-outline" size="36" color="primary" />
              <div>
                <p class="releases-page__card-eyebrow">{{ t('releases.summary.eyebrow') }}</p>
                <p class="releases-page__card-title">{{ t('releases.summary.title') }}</p>
              </div>
            </div>
            <p class="releases-page__card-body">
              {{ t('releases.summary.body') }}
            </p>
            <div class="releases-page__metadata">
              <div>
                <p class="releases-page__meta-label">{{ t('releases.summary.latest') }}</p>
                <p class="releases-page__meta-value">{{ latestReleaseName }}</p>
              </div>
              <div>
                <p class="releases-page__meta-label">{{ t('releases.summary.count') }}</p>
                <p class="releases-page__meta-value">{{ releasesCount }}</p>
              </div>
            </div>
          </v-sheet>
        </v-col>
      </v-row>
    </v-container>

    <v-container class="releases-page__content" max-width="lg">
      <ReleaseAccordion
        :releases="releases"
        :loading="pending"
        :error="error"
        @retry="refresh"
      />
    </v-container>
  </div>
</template>

<script setup lang="ts">
import LatestReleaseBadge from '~/components/domains/releases/LatestReleaseBadge.vue'
import ReleaseAccordion from '~/components/domains/releases/ReleaseAccordion.vue'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

definePageMeta({
  name: 'releases',
  ssr: false,
})

const { t, locale } = useI18n()
const localePath = useLocalePath()
const requestURL = useRequestURL()

const { releases, pending, error, refresh, latestRelease } = await useReleaseNotes()

const releasesCount = computed(() => releases.value.length)
const latestReleaseName = computed(() => latestRelease.value?.name ?? t('releases.empty'))

const canonicalUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('releases', locale.value),
    requestURL.origin
  ).toString()
)

const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

useSeoMeta({
  title: () => String(t('releases.seo.title')),
  description: () => String(t('releases.seo.description')),
  ogTitle: () => String(t('releases.seo.title')),
  ogDescription: () => String(t('releases.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
})

useHead(() => ({
  link: [{ rel: 'canonical', href: canonicalUrl.value }],
}))
</script>

<style scoped lang="sass">
.releases-page
  display: flex
  flex-direction: column
  gap: 32px

  &__hero
    padding-top: 48px
    padding-bottom: 24px

  &__eyebrow
    text-transform: uppercase
    letter-spacing: 0.08em
    color: rgba(var(--v-theme-on-surface), 0.7)
    font-weight: 700
    margin-bottom: 4px

  &__title
    font-size: clamp(2rem, 3vw, 2.6rem)
    font-weight: 800
    margin: 0

  &__subtitle
    font-size: 1.1rem
    color: rgba(var(--v-theme-on-surface), 0.72)
    margin: 0

  &__description
    font-size: 1rem
    color: rgba(var(--v-theme-on-surface), 0.82)
    margin: 0

  &__card
    width: 100%
    padding: 24px
    background: rgba(var(--v-theme-surface-muted-contrast), 0.8)

  &__card-header
    display: flex
    align-items: center
    gap: 12px
    margin-bottom: 12px

  &__card-eyebrow
    margin: 0
    text-transform: uppercase
    letter-spacing: 0.08em
    font-size: 0.78rem
    color: rgba(var(--v-theme-on-surface), 0.6)

  &__card-title
    margin: 0
    font-weight: 700
    font-size: 1.1rem
    color: rgb(var(--v-theme-on-surface))

  &__card-body
    margin: 4px 0 18px
    color: rgba(var(--v-theme-on-surface), 0.75)

  &__metadata
    display: grid
    grid-template-columns: repeat(2, minmax(0, 1fr))
    gap: 12px

  &__meta-label
    margin: 0
    font-size: 0.85rem
    color: rgba(var(--v-theme-on-surface), 0.6)

  &__meta-value
    margin: 4px 0 0
    font-weight: 700
    color: rgb(var(--v-theme-on-surface))

  &__content
    padding-bottom: 56px

@media (max-width: 960px)
  .releases-page
    &__hero
      padding-top: 28px
    &__metadata
      grid-template-columns: repeat(1, minmax(0, 1fr))
</style>
