<template>
  <div class="docs-inline">
    <v-progress-linear
      v-if="pending"
      indeterminate
      color="primary"
      class="docs-inline__loader"
      :aria-label="t('docs.labels.loading')"
    />

    <div v-if="error" class="docs-inline__error" role="alert">
      <v-alert type="error" variant="tonal">
        {{ t('docs.errors.load') }}
      </v-alert>
    </div>

    <div
      v-else-if="doc"
      class="docs-inline__content"
      :class="{
        'docs-prose': renderProse,
        'docs-inline__content--hide-h1': !renderH1,
      }"
      role="article"
      :aria-label="t('docs.labels.article')"
    >
      <ContentRenderer :value="doc" :components="resolvedComponents" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'

import DocsContentLink from '~/components/docs/DocsContentLink.vue'
import {
  normalizeBasePath,
  normalizeDocsLocale,
  resolveDocPath,
  resolveLocaleFromRequest,
  useDocsContent,
  type DocsLocale,
} from '~/composables/useDocsContent'

const props = withDefaults(
  defineProps<{
    slugOrPath: string
    locale?: DocsLocale
    basePath?: string
    renderH1?: boolean
    renderProse?: boolean
    safeLinks?: boolean
    mdcComponents?: Record<string, unknown>
  }>(),
  {
    locale: undefined,
    basePath: '/docs',
    renderH1: false,
    renderProse: true,
    safeLinks: false,
    mdcComponents: () => ({}),
  }
)

const { t } = useI18n()
const { getDocByPath } = useDocsContent()
const resolvedLocale = computed(() =>
  normalizeDocsLocale(props.locale ?? resolveLocaleFromRequest())
)
const resolvedBasePath = computed(() => normalizeBasePath(props.basePath))
const resolvedPath = computed(() =>
  resolveDocPath({
    locale: resolvedLocale.value,
    slugOrPath: props.slugOrPath,
    basePath: resolvedBasePath.value,
  })
)

const { data: doc, pending, error } = await useAsyncData(
  () => `docs-inline:${resolvedPath.value}`,
  () => getDocByPath({ path: resolvedPath.value }),
  { watch: [resolvedPath] }
)

const resolvedComponents = computed(() => ({
  ...(props.mdcComponents ?? {}),
  ...(props.safeLinks
    ? {
        a: (contentProps: { href?: string; target?: string; rel?: string }) =>
          h(DocsContentLink, {
            ...contentProps,
            safeLinks: true,
          }),
      }
    : {}),
}))
</script>

<style scoped>
.docs-inline {
  display: grid;
  gap: 1rem;
}

.docs-inline__content {
  background: rgb(var(--v-theme-surface-default));
  border-radius: 20px;
  padding: clamp(1.25rem, 2vw, 2rem);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
}

.docs-inline__content--hide-h1 :deep(h1) {
  display: none;
}
</style>
