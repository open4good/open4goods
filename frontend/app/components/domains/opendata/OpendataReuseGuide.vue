<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { buildReuseSnippets, fileNameOf } from '~/utils/opendata'

const props = defineProps<{
  /** Public URL of the zip, null while no public base URL is configured. */
  downloadUrl?: string | null
  /** Manifest sha256 of the zip. */
  sha256?: string | null
}>()

const { t } = useI18n()

const snippets = computed(() =>
  props.downloadUrl ? buildReuseSnippets(props.downloadUrl, props.sha256) : null
)
const zipName = computed(() =>
  props.downloadUrl ? fileNameOf(props.downloadUrl) : ''
)

const blocks = computed(() =>
  snippets.value
    ? [
        { id: 'curl', code: snippets.value.curl },
        { id: 'wget', code: snippets.value.wget },
        { id: 'sha256', code: snippets.value.sha256 },
        { id: 'pandas', code: snippets.value.pandas },
        { id: 'duckdb', code: snippets.value.duckdb },
        { id: 'r', code: snippets.value.r },
      ]
    : []
)
</script>

<template>
  <section class="opendata-reuse" aria-labelledby="opendata-reuse-heading">
    <v-container max-width="lg">
      <h2 id="opendata-reuse-heading" class="opendata-reuse__title">
        {{ t('opendata.reuse.title') }}
      </h2>
      <p class="opendata-reuse__subtitle">
        {{ t('opendata.reuse.subtitle') }}
      </p>
      <v-alert
        v-if="!snippets"
        type="info"
        variant="tonal"
        data-testid="opendata-reuse-unavailable"
      >
        {{ t('opendata.reuse.unavailable') }}
      </v-alert>
      <template v-else>
        <p v-if="sha256" class="opendata-reuse__sha">
          {{ t('opendata.reuse.sha256', { file: zipName }) }}
          <code data-testid="opendata-reuse-sha256">{{ sha256 }}</code>
        </p>
        <div
          v-for="block in blocks"
          :key="block.id"
          class="opendata-reuse__block"
        >
          <h3 class="opendata-reuse__block-title">
            {{ t(`opendata.reuse.blocks.${block.id}`) }}
          </h3>
          <pre
            class="opendata-reuse__code"
            tabindex="0"
          ><code>{{ block.code }}</code></pre>
        </div>
      </template>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-reuse
  padding: clamp(2.5rem, 5vw, 4rem) 0
  background: rgba(var(--v-theme-surface-muted), 1)

.opendata-reuse__title
  margin: 0
  font-size: clamp(1.75rem, 3vw, 2.25rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-reuse__subtitle,
.opendata-reuse__sha
  margin: 0.75rem 0 1.5rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.opendata-reuse__sha code
  overflow-wrap: anywhere

.opendata-reuse__block-title
  margin: 1.25rem 0 0.5rem
  font-size: 1.05rem

.opendata-reuse__code
  margin: 0
  padding: 1rem
  border-radius: 12px
  overflow-x: auto
  font-size: 0.9rem
  background: rgba(var(--v-theme-surface-default), 1)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)
</style>
