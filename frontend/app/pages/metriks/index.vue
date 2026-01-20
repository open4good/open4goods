<template>
  <v-container class="metriks-page py-12 py-md-16">
    <header class="metriks-page__header">
      <h1 class="text-h3 font-weight-bold">
        {{ t('metriks.page.title') }}
      </h1>
      <p class="text-body-1 text-medium-emphasis">
        {{ t('metriks.page.description') }}
      </p>
    </header>

    <div class="metriks-page__controls">
      <v-switch
        v-model="includePayload"
        :label="t('metriks.controls.includePayload')"
        color="primary"
        inset
      />
      <v-select
        v-model="limit"
        :items="limitOptions"
        :label="t('metriks.controls.limit')"
        density="comfortable"
        hide-details
        class="metriks-page__limit"
      />
    </div>

    <v-alert v-if="error" type="error" variant="tonal">
      {{ t('metriks.error') }}
    </v-alert>

    <v-progress-linear v-if="pending" indeterminate color="primary" />

    <MetriksTable v-if="report" :report="report" :items-per-page="50" />
  </v-container>
</template>

<script setup lang="ts">
import type { MetriksReportDto } from '~~/shared/api-client'
import MetriksTable from '~/components/metriks/MetriksTable.vue'

definePageMeta({
  ssr: true,
})

const { t } = useI18n()
const canonicalUrl = useCanonicalUrl()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const includePayload = ref(true)
const limit = ref(12)

const limitOptions = [6, 12, 24, 52]

const { data, pending, error } = useAsyncData<MetriksReportDto>(
  'metriks-report',
  () =>
    $fetch('/api/metriks/report', {
      headers: requestHeaders,
      query: {
        includePayload: includePayload.value,
        limit: limit.value,
      },
    }),
  {
    watch: [includePayload, limit],
  }
)

const report = computed(() => data.value ?? { columns: [], rows: [] })

useSeoMeta({
  title: t('metriks.page.seo.title'),
  description: t('metriks.page.seo.description'),
  ogTitle: t('metriks.page.seo.title'),
  ogDescription: t('metriks.page.seo.description'),
  ogUrl: () => canonicalUrl.value ?? undefined,
})

useHead(() => ({
  link: canonicalUrl.value
    ? [{ rel: 'canonical', href: canonicalUrl.value }]
    : [],
}))
</script>

<style scoped>
.metriks-page {
  display: grid;
  gap: 2rem;
}

.metriks-page__header {
  display: grid;
  gap: 0.5rem;
}

.metriks-page__controls {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
}

.metriks-page__limit {
  max-width: 160px;
}
</style>
