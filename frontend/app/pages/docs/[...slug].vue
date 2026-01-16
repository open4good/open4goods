<template>
  <DocsPageRenderer :slug-or-path="resolvedSlug" :locale="resolvedLocale" />
</template>

<script setup lang="ts">
import DocsPageRenderer from '~/components/docs/DocsPageRenderer.vue'
import { resolveLocaleFromRequest } from '~/composables/useDocsContent'

const route = useRoute()

const resolvedSlug = computed(() => {
  const slugParam = route.params.slug
  if (!slugParam) {
    return 'index'
  }

  return Array.isArray(slugParam) ? slugParam.join('/') : slugParam
})

const resolvedLocale = computed(() => resolveLocaleFromRequest())
</script>
