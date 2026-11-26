<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  /** Column names, in the order of the cells of each row. */
  headers: string[]
  /** First rows of the published CSV, read from the sample of the manifest. */
  rows?: string[][] | null
}>()

const { t } = useI18n()

const visibleRows = computed(() => props.rows ?? [])
</script>

<template>
  <section
    v-if="visibleRows.length > 0"
    class="opendata-sample"
    aria-labelledby="opendata-sample-heading"
  >
    <v-container max-width="lg">
      <h2 id="opendata-sample-heading">
        {{ t('opendata.datasets.common.sampleTitle') }}
      </h2>
      <div class="opendata-sample__scroll">
        <v-table density="compact" data-testid="opendata-sample-table">
          <caption>
            {{
              t('opendata.datasets.common.sampleCaption', {
                count: visibleRows.length,
              })
            }}
          </caption>
          <thead>
            <tr>
              <th v-for="header in headers" :key="header" scope="col">
                <code>{{ header }}</code>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in visibleRows" :key="index">
              <td v-for="(cell, cellIndex) in row" :key="cellIndex">
                {{ cell }}
              </td>
            </tr>
          </tbody>
        </v-table>
      </div>
    </v-container>
  </section>
</template>

<style scoped>
.opendata-sample {
  padding-block: 2rem;
}

.opendata-sample__scroll {
  overflow-x: auto;
}

.opendata-sample caption {
  caption-side: bottom;
  padding: 0.5rem;
  text-align: start;
}

.opendata-sample td {
  white-space: nowrap;
  max-width: 24rem;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
