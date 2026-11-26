<template>
  <div class="impact-page">
    <PageHeader
      variant="hero-standard"
      :eyebrow="t('opendata.impact.category.eyebrow')"
      :title="t('opendata.impact.category.title', { vertical: label })"
      :subtitle="t('opendata.impact.category.subtitle', { vertical: label })"
      layout="single-column"
      background="surface-variant"
      surface-variant="orbit"
      container="lg"
      heading-level="h1"
      :heading-id="'impact-dataset-heading'"
      :breadcrumbs="[
        {
          label: String(t('opendata.datasets.common.breadcrumb.label')),
          href: localePath('opendata'),
        },
        {
          label: String(t('opendata.impact.hub.title')),
          href: localePath('opendata-impact'),
        },
      ]"
    />

    <OpendataFreshness :generated-at="dataset?.generatedAt" />

    <OpendataDatasetSummary
      :title="t('opendata.impact.category.summaryTitle')"
      :items="summaryItems"
    />

    <v-container max-width="lg" class="py-4">
      <v-alert type="info" variant="tonal" data-testid="impact-no-score">
        {{ t('opendata.impact.noScore') }}
      </v-alert>
      <div class="d-flex flex-wrap ga-3 mt-4">
        <v-btn
          v-if="categoryHref"
          :to="categoryHref"
          variant="tonal"
          color="primary"
          data-testid="impact-category-link"
        >
          {{ t('opendata.impact.category.categoryLink', { vertical: label }) }}
        </v-btn>
        <v-btn :to="localePath('opendata-impact')" variant="text">
          {{ t('opendata.impact.category.hubLink') }}
        </v-btn>
      </div>
    </v-container>

    <OpendataColumnDictionary
      :title="t('opendata.impact.category.dictionaryTitle')"
      :subtitle="t('opendata.impact.category.dictionarySubtitle')"
      :columns="columns"
      show-source
    />

    <OpendataReuseGuide
      :download-url="dataset?.downloadUrl"
      :sha256="dataset?.sha256"
    />

    <OpendataLicenseSection
      :title="t('opendata.license.title')"
      :description="t('opendata.license.description')"
      :license-label="t('opendata.license.cta.label')"
      :license-aria-label="t('opendata.license.cta.ariaLabel')"
      :license-url="t('opendata.license.cta.href')"
    />

    <OpendataCitation
      :dataset-name="t('opendata.impact.category.title', { vertical: label })"
      :year="generationYear"
      :page-url="canonicalUrl"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OpenDataDatasetDto } from '~~/shared/api-client'

import PageHeader from '~/components/shared/header/PageHeader.vue'
import OpendataFreshness from '~/components/domains/opendata/OpendataFreshness.vue'
import OpendataDatasetSummary from '~/components/domains/opendata/OpendataDatasetSummary.vue'
import OpendataColumnDictionary from '~/components/domains/opendata/OpendataColumnDictionary.vue'
import OpendataReuseGuide from '~/components/domains/opendata/OpendataReuseGuide.vue'
import OpendataLicenseSection from '~/components/domains/opendata/OpendataLicenseSection.vue'
import OpendataCitation from '~/components/domains/opendata/OpendataCitation.vue'
import { useCategories } from '~/composables/categories/useCategories'
import { buildDatasetJsonLd } from '~/utils/opendata'

definePageMeta({
  ssr: true,
})

const { t, locale } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])
const { categories, fetchCategories } = useCategories()

const vertical = computed(() => String(route.params.vertical ?? ''))

const { data: dataset } = await useAsyncData<OpenDataDatasetDto>(
  () => `opendata-impact-${vertical.value}`,
  async () => {
    try {
      return await $fetch<OpenDataDatasetDto>(
        `/api/opendata/impact/${encodeURIComponent(vertical.value)}`,
        { headers: requestHeaders }
      )
    } catch (cause) {
      const statusCode = (cause as { statusCode?: number }).statusCode
      if (statusCode === 404) {
        throw createError({
          statusCode: 404,
          statusMessage: String(t('opendata.impact.category.notFound')),
          fatal: true,
        })
      }
      throw cause
    }
  },
  { watch: [vertical] }
)

// The category page link is optional: do not fail the page without the category list.
await useAsyncData('opendata-impact-categories', async () => {
  await fetchCategories().catch(() => [])
  return true
})

const placeholder = computed(() =>
  String(t('opendata.datasets.common.placeholder'))
)

const label = computed(() => {
  const key = `opendata.impact.verticals.${vertical.value}`
  const text = String(t(key))
  return text === key ? vertical.value : text
})

const categoryHref = computed(() => {
  const category = categories.value.find(
    candidate => candidate.id === vertical.value
  )
  const slug = category?.verticalHomeUrl?.replace(/^\//, '')
  return slug ? `/${slug}` : null
})

const summaryItems = computed(() => [
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

const columns = computed(() => dataset.value?.columns ?? [])

const generationYear = computed(() =>
  dataset.value?.generatedAt
    ? new Date(dataset.value.generatedAt).getUTCFullYear()
    : null
)

const canonicalUrl = computed(() =>
  new URL(
    `${localePath('opendata-impact')}/${vertical.value}`,
    requestURL.origin
  ).toString()
)
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)
const description = computed(() =>
  String(
    t('opendata.impact.category.description', {
      vertical: label.value,
      rows: dataset.value?.recordCount ?? placeholder.value,
    })
  )
)

useSeoMeta({
  title: () =>
    String(t('opendata.impact.category.seoTitle', { vertical: label.value })),
  description: () => description.value,
  ogTitle: () =>
    String(t('opendata.impact.category.seoTitle', { vertical: label.value })),
  ogDescription: () => description.value,
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
          name: String(
            t('opendata.impact.category.title', { vertical: label.value })
          ),
          description: description.value,
          pageUrl: canonicalUrl.value,
          inLanguage: locale.value,
          organizationName: String(t('opendata.datasets.creator.organization')),
          organizationUrl: requestURL.origin,
          spatialCoverage: 'France',
          datasets: dataset.value ? [dataset.value] : [],
          columns: columns.value,
        })
      ),
    },
  ],
}))
</script>

<style scoped lang="sass">
.impact-page
  display: flex
  flex-direction: column
</style>
