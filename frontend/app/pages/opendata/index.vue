<template>
  <div class="opendata-page">
    <OpendataHero
      :eyebrow="t('opendata.hero.eyebrow')"
      :title="t('opendata.hero.title')"
      :subtitle="t('opendata.hero.subtitle')"
      description-bloc-id="webpages:opendata:hero-overview"
      :primary-cta="heroPrimaryCta"
    />

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
      <v-btn color="primary" variant="tonal" @click="refresh">{{ t('common.actions.retry') }}</v-btn>
    </v-container>

    <OpendataDatasetHighlights
      id="datasets"
      :title="t('opendata.datasets.title')"
      :subtitle="t('opendata.datasets.subtitle')"
      :cards="datasetCards"
    />

    <OpendataLicenseSection
      :title="t('opendata.license.title')"
      :description="t('opendata.license.description')"
      :license-label="t('opendata.license.cta.label')"
      :license-aria-label="t('opendata.license.cta.ariaLabel')"
      :license-url="t('opendata.license.cta.href')"
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

import OpendataHero from '~/components/domains/opendata/OpendataHero.vue'
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

const { data, pending, error, refresh } = await useAsyncData<OpenDataOverviewDto>('opendata-overview', () =>
  $fetch<OpenDataOverviewDto>('/api/opendata'),
)

const totalProductValue = computed(() => {
  const count = data.value?.totalProductCount

  if (count === null || count === undefined) {
    return String(t('opendata.stats.placeholder'))
  }

  const numericCount = typeof count === 'number' ? count : Number.parseInt(String(count), 10)

  if (!Number.isFinite(numericCount)) {
    return String(count)
  }

  if (numericCount >= 1_000_000) {
    const millions = Math.floor(numericCount / 1_000_000)
    return String(
      t('opendata.stats.totalProducts.approxMillions', {
        value: new Intl.NumberFormat(locale.value).format(millions),
      }),
    )
  }

  return new Intl.NumberFormat(locale.value).format(numericCount)
})

const stats = computed(() => {
  const overview = data.value

  return [
    {
      icon: 'mdi-database-outline',
      label: String(t('opendata.stats.totalProducts.label')),
      value: totalProductValue.value,
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

const handleHeroPrimaryCtaClick = (event: MouseEvent) => {
  if (!import.meta.client) {
    return
  }

  const targetSection = document.getElementById('datasets')

  if (!targetSection) {
    return
  }

  event.preventDefault()
  targetSection.scrollIntoView({ behavior: 'smooth', block: 'start' })

  if (window.history.replaceState) {
    window.history.replaceState(null, '', '#datasets')
  }
}

const heroPrimaryCta = computed(() => ({
  label: String(t('opendata.hero.primaryCta.label')),
  ariaLabel: String(t('opendata.hero.primaryCta.ariaLabel')),
  href: '#datasets',
  appendIcon: 'mdi-arrow-down',
  onClick: handleHeroPrimaryCtaClick,
}))

const canonicalUrl = computed(
  () => new URL(resolveLocalizedRoutePath('opendata', locale.value), requestURL.origin).toString(),
)
const ogImageUrl = computed(() => new URL('/nudger-icon-512x512.png', requestURL.origin).toString())

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
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
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
</style>

