<template>
  <div class="impact-hub-page">
    <PageHeader
      variant="hero-standard"
      :eyebrow="t('opendata.impact.hub.eyebrow')"
      :title="t('opendata.impact.hub.title')"
      :subtitle="t('opendata.impact.hub.subtitle')"
      layout="single-column"
      background="surface-variant"
      surface-variant="orbit"
      container="lg"
      heading-level="h1"
      :heading-id="'impact-hub-heading'"
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
      :aria-label="t('opendata.loading')"
      role="progressbar"
    />

    <v-container v-if="error" class="py-6" max-width="lg">
      <v-alert type="error" variant="tonal" border="start" role="alert">
        {{ t('opendata.errors.datasetFailed') }}
      </v-alert>
      <v-btn class="mt-4" color="primary" variant="tonal" @click="refresh">{{
        t('common.actions.retry')
      }}</v-btn>
    </v-container>

    <OpendataFreshness :generated-at="oldestGeneratedAt" />

    <v-container max-width="lg" class="py-6">
      <v-alert type="info" variant="tonal" data-testid="impact-no-score">
        {{ t('opendata.impact.noScore') }}
      </v-alert>
    </v-container>

    <section class="impact-hub__list" aria-labelledby="impact-hub-list-heading">
      <v-container max-width="lg">
        <h2 id="impact-hub-list-heading">
          {{ t('opendata.impact.hub.listTitle') }}
        </h2>
        <p v-if="!datasets.length && !pending" data-testid="impact-hub-empty">
          {{ t('opendata.impact.hub.empty') }}
        </p>
        <v-row>
          <v-col
            v-for="dataset in datasets"
            :key="dataset.id"
            cols="12"
            sm="6"
            md="4"
          >
            <v-card
              class="impact-hub__card"
              variant="outlined"
              rounded="xl"
              :to="datasetPath(dataset)"
              :aria-label="
                t('opendata.impact.hub.open', {
                  vertical: verticalLabel(dataset),
                })
              "
            >
              <v-card-item>
                <h3 class="impact-hub__card-title">
                  {{ verticalLabel(dataset) }}
                </h3>
              </v-card-item>
              <v-card-text>
                <dl class="impact-hub__facts">
                  <dt>{{ t('opendata.impact.hub.rows') }}</dt>
                  <dd data-testid="impact-rows">
                    {{ dataset.recordCount ?? placeholder }}
                  </dd>
                  <dt>{{ t('opendata.impact.hub.updated') }}</dt>
                  <dd>{{ dataset.lastUpdated ?? placeholder }}</dd>
                  <dt>{{ t('opendata.impact.hub.size') }}</dt>
                  <dd>{{ dataset.fileSize ?? placeholder }}</dd>
                </dl>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <OpendataMirrors page="impact" />

    <OpendataLicenseSection
      :title="t('opendata.license.title')"
      :description="t('opendata.license.description')"
      :license-label="t('opendata.license.cta.label')"
      :license-aria-label="t('opendata.license.cta.ariaLabel')"
      :license-url="t('opendata.license.cta.href')"
    />

    <OpendataCitation
      :dataset-name="t('opendata.impact.hub.title')"
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
import OpendataMirrors from '~/components/domains/opendata/OpendataMirrors.vue'
import OpendataLicenseSection from '~/components/domains/opendata/OpendataLicenseSection.vue'
import OpendataCitation from '~/components/domains/opendata/OpendataCitation.vue'
import { buildDatasetJsonLd, verticalOfDatasetId } from '~/utils/opendata'

definePageMeta({
  ssr: true,
})

const { t, locale } = useI18n()
const localePath = useLocalePath()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const { data, pending, error, refresh } = useAsyncData<OpenDataDatasetDto[]>(
  'opendata-impact-hub',
  () =>
    $fetch<OpenDataDatasetDto[]>('/api/opendata/impact', {
      headers: requestHeaders,
    })
)

const datasets = computed(() => data.value ?? [])
const placeholder = computed(() =>
  String(t('opendata.datasets.common.placeholder'))
)

const verticalLabel = (dataset: OpenDataDatasetDto): string => {
  const vertical = verticalOfDatasetId(dataset.id)
  const key = `opendata.impact.verticals.${vertical}`
  const label = String(t(key))
  return label === key ? vertical : label
}

const datasetPath = (dataset: OpenDataDatasetDto) =>
  `${localePath('opendata-impact')}/${verticalOfDatasetId(dataset.id)}`

// The hub is as fresh as its oldest file.
const oldestGeneratedAt = computed(
  () =>
    datasets.value
      .map(dataset => dataset.generatedAt)
      .filter((value): value is string => Boolean(value))
      .sort()[0]
)
const generationYear = computed(() => {
  const latest = datasets.value
    .map(dataset => dataset.generatedAt)
    .filter((value): value is string => Boolean(value))
    .sort()
    .pop()
  return latest ? new Date(latest).getUTCFullYear() : null
})

const canonicalUrl = computed(() =>
  new URL(localePath('opendata-impact'), requestURL.origin).toString()
)
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

useSeoMeta({
  title: () => String(t('opendata.impact.hub.seoTitle')),
  description: () => String(t('opendata.impact.hub.description')),
  ogTitle: () => String(t('opendata.impact.hub.seoTitle')),
  ogDescription: () => String(t('opendata.impact.hub.description')),
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
          name: String(t('opendata.impact.hub.title')),
          description: String(t('opendata.impact.hub.description')),
          pageUrl: canonicalUrl.value,
          inLanguage: locale.value,
          organizationName: String(t('opendata.datasets.creator.organization')),
          organizationUrl: requestURL.origin,
          spatialCoverage: 'France',
          datasets: datasets.value,
        })
      ),
    },
  ],
}))
</script>

<style scoped lang="sass">
.impact-hub-page
  display: flex
  flex-direction: column

.impact-hub__list
  padding: clamp(2rem, 4vw, 3rem) 0

.impact-hub__list h2
  margin: 0 0 1.5rem
  font-size: clamp(1.75rem, 3vw, 2.25rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.impact-hub__card-title
  margin: 0
  font-size: 1.25rem

.impact-hub__facts
  display: grid
  grid-template-columns: auto 1fr
  gap: 0.25rem 1rem
  margin: 0

.impact-hub__facts dt
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.impact-hub__facts dd
  margin: 0
  font-weight: 600
  text-align: right
</style>
