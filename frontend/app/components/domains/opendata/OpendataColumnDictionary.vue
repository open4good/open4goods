<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { formatFillRate } from '~/utils/opendata'

export interface DictionaryColumn {
  name: string
  description?: string | null
  unit?: string | null
  source?: string | null
  /** Share of filled rows between 0 and 1, from the manifest. */
  fillRate?: number
}

defineProps<{
  title: string
  subtitle?: string
  columns: DictionaryColumn[]
  /** Show the origin column (impact datasets). */
  showSource?: boolean
}>()

const { t } = useI18n()
</script>

<template>
  <section
    class="opendata-dictionary"
    aria-labelledby="opendata-dictionary-heading"
  >
    <v-container max-width="lg">
      <div class="opendata-dictionary__header">
        <h2 id="opendata-dictionary-heading">{{ title }}</h2>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <v-table class="opendata-dictionary__table" density="comfortable">
        <thead>
          <tr>
            <th scope="col">{{ t('opendata.datasets.common.columnName') }}</th>
            <th scope="col">
              {{ t('opendata.datasets.common.columnDescription') }}
            </th>
            <th scope="col">{{ t('opendata.dictionary.unit') }}</th>
            <th v-if="showSource" scope="col">
              {{ t('opendata.dictionary.source') }}
            </th>
            <th scope="col">{{ t('opendata.dictionary.fillRate') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="column in columns" :key="column.name">
            <th scope="row">
              <code>{{ column.name }}</code>
            </th>
            <td>{{ column.description ?? '' }}</td>
            <td>{{ column.unit ?? '' }}</td>
            <td v-if="showSource">{{ column.source ?? '' }}</td>
            <td data-testid="opendata-fill-rate">
              <template v-if="formatFillRate(column.fillRate) !== null">
                {{ formatFillRate(column.fillRate) }} %
              </template>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-dictionary
  padding: clamp(3rem, 6vw, 4.5rem) 0
  background: rgba(var(--v-theme-surface-default), 1)

.opendata-dictionary__header
  max-width: 720px
  margin: 0 auto 2rem
  text-align: center

.opendata-dictionary__header h2
  margin: 0
  font-size: clamp(1.9rem, 3vw, 2.4rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-dictionary__header p
  margin: 1rem 0 0
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.opendata-dictionary__table
  border-radius: 20px
  overflow: hidden
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
</style>
