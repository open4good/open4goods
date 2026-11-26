<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { OPENDATA_DOI } from '~/utils/opendata'

const props = defineProps<{
  /** Name of the dataset as cited, e.g. "Nudger : base GTIN". */
  datasetName: string
  /** Year of the generation date, when known. */
  year?: number | null
  pageUrl: string
}>()

const { t } = useI18n()

const doiUrl = `https://doi.org/${OPENDATA_DOI}`
const citation = computed(() =>
  t('opendata.citation.text', {
    name: props.datasetName,
    year: props.year ?? t('opendata.citation.noYear'),
    doi: doiUrl,
    url: props.pageUrl,
  })
)
</script>

<template>
  <section
    class="opendata-citation"
    aria-labelledby="opendata-citation-heading"
  >
    <v-container max-width="lg">
      <h2 id="opendata-citation-heading">{{ t('opendata.citation.title') }}</h2>
      <p>
        {{ t('opendata.citation.doiIntro') }}
        <a :href="doiUrl" target="_blank" rel="noopener">{{ OPENDATA_DOI }}</a>
      </p>
      <blockquote data-testid="opendata-citation-text">
        {{ citation }}
      </blockquote>
      <h3>{{ t('opendata.citation.attributionTitle') }}</h3>
      <p>{{ t('opendata.citation.attribution') }}</p>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-citation
  padding: clamp(2.5rem, 5vw, 4rem) 0

.opendata-citation h2
  margin: 0 0 1rem
  font-size: clamp(1.75rem, 3vw, 2.25rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-citation blockquote
  margin: 1rem 0
  padding: 1rem 1.25rem
  border-left: 4px solid rgba(var(--v-theme-primary), 1)
  background: rgba(var(--v-theme-surface-muted), 1)
  overflow-wrap: anywhere
</style>
