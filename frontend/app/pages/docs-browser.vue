<template>
  <v-container class="docs-browser-page py-12 py-md-16">
    <header class="docs-browser-page__header">
      <h1 class="text-h3 font-weight-bold">
        {{ t('docs.browser.title') }}
      </h1>
      <p class="text-body-1 text-medium-emphasis">
        {{ t('docs.browser.description') }}
      </p>
    </header>

    <DocBrowser :locale="resolvedLocale" base-path="/docs" />
  </v-container>
</template>

<script setup lang="ts">
import DocBrowser from '~/components/docs/DocBrowser.vue'
import { resolveLocaleFromRequest } from '~/composables/useDocsContent'

const { t } = useI18n()
const canonicalUrl = useCanonicalUrl()
const resolvedLocale = computed(() => resolveLocaleFromRequest())

useSeoMeta({
  title: t('docs.browser.seo.title'),
  description: t('docs.browser.seo.description'),
  ogTitle: t('docs.browser.seo.title'),
  ogDescription: t('docs.browser.seo.description'),
  ogUrl: () => canonicalUrl.value ?? undefined,
})

useHead(() => ({
  link: canonicalUrl.value
    ? [{ rel: 'canonical', href: canonicalUrl.value }]
    : [],
}))
</script>

<style scoped>
.docs-browser-page {
  display: grid;
  gap: 2rem;
}

.docs-browser-page__header {
  display: grid;
  gap: 0.5rem;
}
</style>
