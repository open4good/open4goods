<script setup lang="ts">
import { computed } from 'vue'
import { getDomainLanguageFromHostname } from '~~/shared/utils/domain-language'

let requestURL: URL | null = null

try {
  requestURL = useRequestURL()
} catch {
  requestURL = null
}

const documentLanguage = computed(
  () => getDomainLanguageFromHostname(requestURL?.hostname ?? null).domainLanguage,
)

useHead(() => ({
  htmlAttrs: {
    lang: documentLanguage.value,
  },
}))
</script>

<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>
