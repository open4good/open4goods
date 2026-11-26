<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import {
  OPENDATA_STALE_AFTER_DAYS,
  datasetAgeInDays,
  isDatasetStale,
} from '~/utils/opendata'

const props = defineProps<{
  /** Generation date of the published file, ISO 8601 (manifest `generatedAt`). */
  generatedAt?: string | null
}>()

const { t } = useI18n()

// One reference instant per request, serialised in the payload: SSR and hydration compute the same age.
const nowIso = useState('opendata-now', () => new Date().toISOString())
const now = computed(() => new Date(nowIso.value))

const ageDays = computed(() => datasetAgeInDays(props.generatedAt, now.value))
const stale = computed(() => isDatasetStale(props.generatedAt, now.value))
</script>

<template>
  <v-container v-if="ageDays !== null" max-width="lg" class="pb-0">
    <v-chip
      class="opendata-freshness__badge"
      :color="stale ? 'warning' : 'success'"
      variant="tonal"
      prepend-icon="mdi-calendar-clock"
      data-testid="opendata-freshness-badge"
    >
      {{
        ageDays === 0
          ? t('opendata.freshness.today')
          : t('opendata.freshness.badge', { days: ageDays })
      }}
    </v-chip>
    <v-alert
      v-if="stale"
      class="mt-4"
      type="warning"
      variant="tonal"
      border="start"
      role="status"
      data-testid="opendata-freshness-banner"
    >
      {{ t('opendata.freshness.stale', { days: OPENDATA_STALE_AFTER_DAYS }) }}
    </v-alert>
  </v-container>
</template>
