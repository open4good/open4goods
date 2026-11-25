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
  () =>
    getDomainLanguageFromHostname(requestURL?.hostname ?? null).domainLanguage
)

const clientErrorToast = useState<string | null>(
  'client-error-toast',
  () => null
)
const isClientErrorToastOpen = computed({
  get: () => Boolean(clientErrorToast.value),
  set: open => {
    if (!open) {
      clientErrorToast.value = null
    }
  },
})

useHead(() => ({
  htmlAttrs: {
    lang: documentLanguage.value || 'fr',
  },
}))
</script>

<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
  <v-snackbar v-model="isClientErrorToastOpen" color="error" role="alert">
    {{ clientErrorToast }}
  </v-snackbar>
</template>
