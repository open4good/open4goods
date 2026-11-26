<template>
  <div class="dataset-page">
    <PageHeader
      variant="hero-standard"
      :eyebrow="t('opendata.datasets.isbn.hero.eyebrow')"
      :title="t('opendata.datasets.isbn.hero.title')"
      :description-bloc-id="'webpages:opendata:isbn-hero-overview'"
      layout="single-column"
      background="surface-variant"
      surface-variant="orbit"
      container="lg"
      heading-level="h1"
      :heading-id="'dataset-hero-heading'"
      :breadcrumbs="[
        {
          label: String(t('opendata.datasets.common.breadcrumb.label')),
          href: localePath('opendata'),
        },
      ]"
    />

    <v-progress-linear
      v-if="pending"
      indeterminate
      color="primary"
      class="dataset-page__loader"
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
        {{ t('opendata.errors.datasetFailed') }}
      </v-alert>
      <v-btn color="primary" variant="tonal" @click="refresh">{{
        t('common.actions.retry')
      }}</v-btn>
    </v-container>

    <OpendataFreshness :generated-at="dataset?.generatedAt" />

    <OpendataDatasetSummary
      :title="t('opendata.datasets.isbn.summary.title')"
      :items="summaryItems"
    />

    <section class="dataset-format" aria-labelledby="dataset-format-heading">
      <v-container max-width="lg">
        <div class="dataset-format__card">
          <div class="dataset-format__header">
            <h2 id="dataset-format-heading">
              {{ t('opendata.datasets.isbn.format.title') }}
            </h2>
            <p>{{ t('opendata.datasets.isbn.format.description') }}</p>
          </div>
          <ul class="dataset-format__list">
            <li v-for="item in formatBullets" :key="item">
              <v-icon icon="mdi-check-circle-outline" size="small" />
              <span>{{ item }}</span>
            </li>
          </ul>
        </div>
      </v-container>
    </section>

    <OpendataColumnDictionary
      :title="t('opendata.datasets.isbn.columnsTitle')"
      :subtitle="t('opendata.datasets.isbn.columnsSubtitle')"
      :columns="columnDefinitions"
    />

    <OpendataDownloadComparison
      :title="t('opendata.datasets.isbn.download.title')"
      :subtitle="t('opendata.datasets.isbn.download.subtitle')"
      :options="downloadOptions"
    />

    <OpendataReuseGuide
      :download-url="dataset?.downloadUrl"
      :sha256="dataset?.sha256"
    />

    <OpendataExploreProducts page="isbn" />

    <OpendataMirrors page="isbn" />

    <OpendataCitation
      :dataset-name="t('opendata.datasets.isbn.seo.title')"
      :year="generationYear"
      :page-url="canonicalUrl"
    />

    <OpendataLicenseSection
      :title="t('opendata.license.title')"
      :description="t('opendata.license.description')"
      :license-label="t('opendata.license.cta.label')"
      :license-aria-label="t('opendata.license.cta.ariaLabel')"
      :license-url="t('opendata.license.cta.href')"
    />

    <OpendataFaqSection
      :title="t('opendata.datasets.isbn.faq.title')"
      :subtitle="t('opendata.datasets.isbn.faq.subtitle')"
      :items="faqItems"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OpenDataDatasetDto } from '~~/shared/api-client'

import PageHeader from '~/components/shared/header/PageHeader.vue'
import OpendataDatasetSummary from '~/components/domains/opendata/OpendataDatasetSummary.vue'
import OpendataDownloadComparison from '~/components/domains/opendata/OpendataDownloadComparison.vue'
import OpendataExploreProducts from '~/components/domains/opendata/OpendataExploreProducts.vue'
import OpendataMirrors from '~/components/domains/opendata/OpendataMirrors.vue'
import OpendataFaqSection from '~/components/domains/opendata/OpendataFaqSection.vue'
import OpendataFreshness from '~/components/domains/opendata/OpendataFreshness.vue'
import OpendataColumnDictionary from '~/components/domains/opendata/OpendataColumnDictionary.vue'
import OpendataReuseGuide from '~/components/domains/opendata/OpendataReuseGuide.vue'
import OpendataCitation from '~/components/domains/opendata/OpendataCitation.vue'
import { OPENDATA_DOI, buildDatasetJsonLd } from '~/utils/opendata'
import OpendataLicenseSection from '~/components/domains/opendata/OpendataLicenseSection.vue'

definePageMeta({
  ssr: true,
})

const { t, locale } = useI18n()
const localePath = useLocalePath()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

interface DatasetPayload {
  dataset: OpenDataDatasetDto
}

const { data, pending, error, refresh } = useAsyncData<DatasetPayload>(
  'opendata-isbn',
  async () => {
    const dataset = await $fetch<OpenDataDatasetDto>('/api/opendata/isbn', {
      headers: requestHeaders,
    })

    return { dataset }
  },
  {
    lazy: true,
  }
)

const dataset = computed(() => data.value?.dataset)
const placeholder = computed(() =>
  String(t('opendata.datasets.common.placeholder'))
)

const summaryItems = computed(() => [
  {
    label: String(t('opendata.datasets.common.summary.format')),
    value: String(t('opendata.datasets.common.summary.formatValue')),
    icon: 'mdi-folder-zip-outline',
  },
  {
    label: String(t('opendata.datasets.common.summary.records')),
    value: dataset.value?.recordCount ?? placeholder.value,
    icon: 'mdi-database-outline',
  },
  {
    label: String(t('opendata.datasets.common.summary.updated')),
    value: dataset.value?.lastUpdated ?? placeholder.value,
    icon: 'mdi-calendar-clock',
  },
  {
    label: String(t('opendata.datasets.common.summary.size')),
    value: dataset.value?.fileSize ?? placeholder.value,
    icon: 'mdi-package-down',
  },
])

const formatBullets = computed(() => [
  String(t('opendata.datasets.isbn.format.bullets.type')),
  String(t('opendata.datasets.isbn.format.bullets.separator')),
  String(t('opendata.datasets.isbn.format.bullets.quote')),
])

const downloadOptions = computed(() => {
  const directUrl = dataset.value?.downloadUrl
  const sha256 = dataset.value?.sha256

  return [
    {
      id: 'datagouv',
      title: String(t('opendata.datasets.common.download.fast.title')),
      description: String(
        t('opendata.datasets.common.download.fast.description')
      ),
      badge: String(t('opendata.datasets.common.download.fast.badge')),
      highlights: [
        {
          icon: 'mdi-school-outline',
          text: String(t('opendata.datasets.isbn.download.fast.highlight')),
        },
      ],
      cta: {
        label: String(t('opendata.datasets.common.download.fast.cta.label')),
        ariaLabel: String(
          t('opendata.datasets.common.download.fast.cta.ariaLabel')
        ),
        href: String(t('opendata.datasets.common.download.fast.cta.href')),
        target: '_blank',
        rel: 'noopener nofollow',
      },
    },
    {
      id: 'direct',
      title: String(t('opendata.datasets.common.download.direct.title')),
      description: String(
        t('opendata.datasets.common.download.direct.description')
      ),
      highlights: [
        {
          icon: 'mdi-restart',
          text: String(t('opendata.datasets.common.download.direct.resume')),
        },
        ...(sha256
          ? [
              {
                icon: 'mdi-fingerprint',
                text: String(
                  t('opendata.datasets.common.download.direct.checksum', {
                    value: sha256,
                  })
                ),
              },
            ]
          : []),
      ],
      cta: {
        label: String(t('opendata.datasets.common.download.direct.cta.label')),
        ariaLabel: String(
          t('opendata.datasets.common.download.direct.cta.ariaLabel')
        ),
        href: directUrl ?? '#',
        disabled: !directUrl,
      },
    },
  ]
})

const columnDefinitions = computed(() => {
  const headers = dataset.value?.headers ?? []
  const manifestColumns = new Map(
    (dataset.value?.columns ?? []).map(column => [column.name, column])
  )

  return headers.map(header => {
    const manifestColumn = manifestColumns.get(header)
    return {
      name: header,
      description: String(
        t(`opendata.datasets.isbn.columns.${header}.description` as const)
      ),
      unit: manifestColumn?.unit,
      fillRate: manifestColumn?.fillRate,
    }
  })
})

const generationYear = computed(() => {
  const year = dataset.value?.generatedAt
    ? new Date(dataset.value.generatedAt).getUTCFullYear()
    : null
  return year && !Number.isNaN(year) ? year : null
})

const faqItems = computed(() => [
  {
    id: 'what-is-isbn',
    question: String(t('opendata.datasets.isbn.faq.items.whatIs.question')),
    answer: String(t('opendata.datasets.isbn.faq.items.whatIs.answer')),
  },
  {
    id: 'structure',
    question: String(t('opendata.datasets.isbn.faq.items.structure.question')),
    answer: String(t('opendata.datasets.isbn.faq.items.structure.answer')),
  },
  {
    id: 'assignment',
    question: String(t('opendata.datasets.isbn.faq.items.assignment.question')),
    answer: String(t('opendata.datasets.isbn.faq.items.assignment.answer')),
  },
  {
    id: 'difference',
    question: String(t('opendata.datasets.isbn.faq.items.difference.question')),
    answer: String(t('opendata.datasets.isbn.faq.items.difference.answer')),
  },
  {
    id: 'importance',
    question: String(t('opendata.datasets.isbn.faq.items.importance.question')),
    answer: String(t('opendata.datasets.isbn.faq.items.importance.answer')),
  },
  {
    id: 'non-book',
    question: String(t('opendata.datasets.isbn.faq.items.nonBook.question')),
    answer: String(t('opendata.datasets.isbn.faq.items.nonBook.answer')),
  },
])

const canonicalUrl = computed(() =>
  new URL(
    // `opendata-isbn` is a file-based route, absent from LOCALIZED_ROUTE_PATHS:
    // resolveLocalizedRoutePath would return the 404 path `/opendata-isbn`.
    localePath('opendata-isbn'),
    requestURL.origin
  ).toString()
)
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

useSeoMeta({
  title: () => String(t('opendata.datasets.isbn.seo.title')),
  description: () => String(t('opendata.datasets.isbn.seo.description')),
  ogTitle: () => String(t('opendata.datasets.isbn.seo.title')),
  ogDescription: () => String(t('opendata.datasets.isbn.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
})

useHead(() => ({
  link: [{ rel: 'canonical', href: canonicalUrl.value }],
  script: [
    {
      type: 'application/ld+json',
      innerHTML: JSON.stringify(
        buildDatasetJsonLd({
          name: String(t('opendata.datasets.isbn.seo.title')),
          description: String(t('opendata.datasets.isbn.seo.description')),
          pageUrl: canonicalUrl.value,
          inLanguage: locale.value,
          organizationName: String(t('opendata.datasets.creator.organization')),
          organizationUrl: requestURL.origin,
          doi: OPENDATA_DOI,
          datasets: dataset.value ? [dataset.value] : [],
          columns: columnDefinitions.value.map(column => ({
            name: column.name,
            description: column.description,
            unit: column.unit,
          })),
        })
      ),
    },
  ],
}))
</script>

<style scoped lang="sass">
.dataset-page
  display: flex
  flex-direction: column
  gap: 0

.dataset-page__loader
  margin: 0

.dataset-format
  padding: clamp(2.5rem, 5vw, 4rem) 0
  background: rgba(var(--v-theme-surface-muted), 1)

.dataset-format__card
  border-radius: 24px
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)
  background: rgba(var(--v-theme-surface-default), 0.95)
  padding: clamp(1.75rem, 4vw, 2.75rem)
  display: flex
  flex-direction: column
  gap: 1.5rem

.dataset-format__header h2
  margin: 0
  font-size: clamp(1.75rem, 3vw, 2.25rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.dataset-format__header p
  margin: 0.5rem 0 0
  font-size: 1rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.dataset-format__list
  list-style: none
  margin: 0
  padding: 0
  display: flex
  flex-direction: column
  gap: 0.75rem

.dataset-format__list li
  display: flex
  gap: 0.75rem
  align-items: center
  font-size: 0.95rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)
</style>
