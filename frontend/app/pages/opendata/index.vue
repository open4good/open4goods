<template>
  <div class="opendata-page">
    <PageHeader
      variant="hero-standard"
      :eyebrow="t('opendata.hero.eyebrow')"
      :title="t('opendata.hero.title')"
      :subtitle="t('opendata.hero.subtitle')"
      layout="2-columns"
      background="image"
      background-image-asset-key="openDataBackground"
      surface-variant="halo"
      container="lg"
      show-media
      media-type="card"
      :hero-card="educationCard"
      :primary-cta="heroPrimaryCta"
      heading-level="h1"
      :heading-id="'opendata-hero-heading'"
      schema-type="DataCatalog"
      :og-image="ogImageUrl"
    >
      <template #media>
        <div class="opendata-hero__visual">
          <div class="opendata-hero__glow" aria-hidden="true">
            <div class="opendata-hero__glow-ring" />
            <div
              class="opendata-hero__glow-ring opendata-hero__glow-ring--secondary"
            />
            <span v-for="index in 6" :key="index" />
          </div>

          <HeroEducationCard
            class="opendata-hero__education-card"
            v-bind="educationCard"
          />
        </div>
      </template>
    </PageHeader>

    <OpendataStatsStrip :stats="stats" />

    <v-progress-linear
      v-if="pending"
      indeterminate
      color="primary"
      class="opendata-page__loader"
      :aria-label="t('opendata.loading')"
      role="progressbar"
    />

    <v-container v-if="error" class="py-6" max-width="lg">
      <v-alert
        type="error"
        variant="tonal"
        border="start"
        prominent
        class="mb-4"
        role="alert"
      >
        {{ t('opendata.errors.overviewFailed') }}
      </v-alert>
      <v-btn color="primary" variant="tonal" @click="refresh">{{
        t('common.actions.retry')
      }}</v-btn>
    </v-container>

    <OpendataDatasetHighlights
      id="datasets"
      :title="t('opendata.datasets.title')"
      :cards="datasetCards"
    >
      <template #subtitle>
        <div class="opendata-datasets__subtitle subtitle-text">
          <i18n-t keypath="opendata.datasets.subtitle" tag="p">
            <template #license>
              <a
                :href="`#${licenseSectionId}`"
                :data-scroll-target="`#${licenseSectionId}`"
                >ODbL</a
              >
            </template>
          </i18n-t>
        </div>
      </template>
    </OpendataDatasetHighlights>

    <OpendataLicenseSection
      :title="t('opendata.license.title')"
      :description="t('opendata.license.description')"
      :license-label="t('opendata.license.cta.label')"
      :license-aria-label="t('opendata.license.cta.ariaLabel')"
      :license-url="t('opendata.license.cta.href')"
      :license-id="licenseSectionId"
    />

    <OpendataFaqSection
      :title="t('opendata.faq.title')"
      :subtitle="t('opendata.faq.subtitle')"
      :items="faqItems"
    />

    <OpendataOpenSourceStrip
      :title="t('opendata.opensource.title')"
      :description="t('opendata.opensource.description')"
      :cta-label="t('opendata.opensource.cta.label')"
      :cta-aria-label="t('opendata.opensource.cta.ariaLabel')"
      :href="localePath('opensource')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OpenDataOverviewDto } from '~~/shared/api-client'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

import PageHeader from '~/components/shared/header/PageHeader.vue'
import HeroEducationCard from '~/components/shared/ui/HeroEducationCard.vue'
import OpendataStatsStrip from '~/components/domains/opendata/OpendataStatsStrip.vue'
import OpendataDatasetHighlights from '~/components/domains/opendata/OpendataDatasetHighlights.vue'
import OpendataLicenseSection from '~/components/domains/opendata/OpendataLicenseSection.vue'
import OpendataFaqSection from '~/components/domains/opendata/OpendataFaqSection.vue'
import OpendataOpenSourceStrip from '~/components/domains/opendata/OpendataOpenSourceStrip.vue'

definePageMeta({
  ssr: true,
})

const { t, locale } = useI18n()
const localePath = useLocalePath()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])
const licenseSectionId = 'opendata-odbl-license'

const { data, pending, error, refresh } = useAsyncData<OpenDataOverviewDto>(
  'opendata-overview',
  () =>
    $fetch<OpenDataOverviewDto>('/api/opendata', {
      headers: requestHeaders,
    }),
  {
    lazy: true,
  }
)

const formatProductCount = (count?: string | number | null) => {
  if (count == null || count === '') {
    return String(t('opendata.stats.placeholder'))
  }

  const numericValue =
    typeof count === 'number'
      ? count
      : Number(String(count).replace(/[^\d]/g, ''))

  if (!Number.isFinite(numericValue) || numericValue <= 0) {
    return String(t('opendata.stats.placeholder'))
  }

  if (numericValue >= 1_000_000) {
    const millions = Math.round(numericValue / 1_000_000)
    return String(
      t('opendata.stats.totalProducts.millions', { value: millions })
    )
  }

  return new Intl.NumberFormat(locale.value).format(numericValue)
}

const stats = computed(() => {
  const overview = data.value

  return [
    {
      icon: 'mdi-database-outline',
      label: String(t('opendata.stats.totalProducts.label')),
      value: formatProductCount(overview?.totalProductCount),
      description: String(t('opendata.stats.totalProducts.description')),
    },
    {
      icon: 'mdi-file-table-box-multiple-outline',
      label: String(t('opendata.stats.datasets.label')),
      value: overview?.datasetCount ?? t('opendata.stats.placeholder'),
      description: String(t('opendata.stats.datasets.description')),
    },
    {
      icon: 'mdi-cloud-download-outline',
      label: String(t('opendata.stats.totalSize.label')),
      value: overview?.totalDatasetSize ?? t('opendata.stats.placeholder'),
      description: String(t('opendata.stats.totalSize.description')),
    },
  ]
})

const datasetCards = computed(() => [
  {
    id: 'gtin',
    icon: 'mdi-barcode-scan',
    title: String(t('opendata.datasets.cards.gtin.title')),
    description: String(t('opendata.datasets.cards.gtin.description')),
    features: [
      {
        icon: 'mdi-refresh',
        text: String(t('opendata.datasets.cards.gtin.features.refresh')),
      },
      {
        icon: 'mdi-chart-box-outline',
        text: String(t('opendata.datasets.cards.gtin.features.coverage')),
      },
      {
        icon: 'mdi-account-group-outline',
        text: String(t('opendata.datasets.cards.gtin.features.uses')),
      },
    ],
    ctaLabel: String(t('opendata.datasets.cards.gtin.cta.label')),
    ctaAriaLabel: String(t('opendata.datasets.cards.gtin.cta.ariaLabel')),
    href: localePath('opendata-gtin'),
  },
  {
    id: 'isbn',
    icon: 'mdi-book-open-variant',
    title: String(t('opendata.datasets.cards.isbn.title')),
    description: String(t('opendata.datasets.cards.isbn.description')),
    features: [
      {
        icon: 'mdi-refresh',
        text: String(t('opendata.datasets.cards.isbn.features.refresh')),
      },
      {
        icon: 'mdi-library-outline',
        text: String(t('opendata.datasets.cards.isbn.features.catalogue')),
      },
      {
        icon: 'mdi-school-outline',
        text: String(t('opendata.datasets.cards.isbn.features.research')),
      },
    ],
    ctaLabel: String(t('opendata.datasets.cards.isbn.cta.label')),
    ctaAriaLabel: String(t('opendata.datasets.cards.isbn.cta.ariaLabel')),
    href: localePath('opendata-isbn'),
  },
])

const faqItems = computed(() => [
  {
    id: 'sources',
    question: String(t('opendata.faq.items.sources.question')),
    answer: String(t('opendata.faq.items.sources.answer')),
  },
  {
    id: 'definition',
    question: String(t('opendata.faq.items.definition.question')),
    answer: String(t('opendata.faq.items.definition.answer')),
  },
  {
    id: 'importance',
    question: String(t('opendata.faq.items.importance.question')),
    answer: String(t('opendata.faq.items.importance.answer')),
  },
  {
    id: 'contributors',
    question: String(t('opendata.faq.items.contributors.question')),
    answer: String(t('opendata.faq.items.contributors.answer')),
  },
])

const heroPrimaryCta = computed(() => ({
  label: String(t('opendata.hero.primaryCta.label')),
  ariaLabel: String(t('opendata.hero.primaryCta.ariaLabel')),
  href: '#datasets',
  appendIcon: 'mdi-arrow-down',
}))

const educationCard = computed(() => ({
  icon: 'mdi-database-check-outline',
  title: String(t('opendata.hero.educationCard.title')),
  bodyHtml: String(t('opendata.hero.educationCard.description')),
}))

const canonicalUrl = computed(() =>
  new URL(
    resolveLocalizedRoutePath('opendata', locale.value),
    requestURL.origin
  ).toString()
)
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

useSeoMeta({
  title: () => String(t('opendata.seo.title')),
  description: () => String(t('opendata.seo.description')),
  ogTitle: () => String(t('opendata.seo.title')),
  ogDescription: () => String(t('opendata.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
})

useHead(() => ({
  link: [{ rel: 'canonical', href: canonicalUrl.value }],
  script: [
    {
      type: 'application/ld+json',
      children: JSON.stringify({
        '@context': 'https://schema.org',
        '@type': 'DataCatalog',
        name: t('opendata.seo.title'),
        description: t('opendata.seo.description'),
        url: canonicalUrl.value,
        dataset: datasetCards.value.map(card => ({
          '@type': 'Dataset',
          name: card.title,
          description: card.description,
          url: new URL(card.href, requestURL.origin).toString(),
          isAccessibleForFree: true,
          license: 'https://opendatacommons.org/licenses/odbl/',
        })),
      }),
    },
  ],
}))
</script>

<style scoped lang="sass">
.opendata-page
  display: flex
  flex-direction: column
  gap: 0

.opendata-page__loader
  margin: 0

.opendata-hero__visual
  display: flex
  justify-content: center
  align-items: center
  position: relative
  width: 100%

.opendata-hero__glow
  position: relative
  width: min(320px, 100%)
  aspect-ratio: 1

.opendata-hero__glow-ring
  position: absolute
  inset: 10%
  border-radius: 50%
  border: 1px solid rgba(var(--v-theme-hero-overlay-strong), 0.35)
  box-shadow: 0 0 60px rgba(var(--v-theme-accent-primary-highlight), 0.25)

.opendata-hero__glow-ring--secondary
  inset: 20%
  border-color: rgba(var(--v-theme-accent-supporting), 0.3)
  box-shadow: 0 0 40px rgba(var(--v-theme-accent-supporting), 0.22)

.opendata-hero__education-card
  position: relative
  z-index: 1
  margin-top: clamp(1rem, 3vw, 2rem)
  box-shadow: 0 20px 60px rgba(var(--v-theme-shadow-primary-600), 0.18)

@media (max-width: 959px)
  .opendata-hero__visual
    margin-top: 2rem
    align-items: stretch

  .opendata-hero__glow
    width: 240px
    margin-inline: auto

  .opendata-hero__education-card
    max-width: none
</style>
