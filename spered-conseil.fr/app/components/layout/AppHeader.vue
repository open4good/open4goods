<template>
  <v-app-bar flat class="app-header">
    <v-container class="d-flex align-center justify-space-between">
      <div class="text-h6 font-weight-bold">{{ t('header.brand') }}</div>
      <div class="d-flex ga-2 align-center">
        <v-btn variant="text" to="/">{{ t('header.home') }}</v-btn>
        <v-btn variant="text" to="/contact">{{ t('header.contact') }}</v-btn>
        <v-btn-toggle v-model="selectedLocale" mandatory density="compact" @update:model-value="switchLocale">
          <v-btn value="fr-FR">FR</v-btn>
          <v-btn value="en-US">EN</v-btn>
        </v-btn-toggle>
      </div>
    </v-container>
  </v-app-bar>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const runtimeConfig = useRuntimeConfig()
const { locale, t } = useI18n()

const selectedLocale = ref(locale.value)

watch(
  () => locale.value,
  newLocale => {
    selectedLocale.value = newLocale
  }
)

const switchLocale = async (value: string) => {
  if (!value || value === locale.value) {
    return
  }

  locale.value = value

  if (import.meta.client) {
    const targetDomain =
      value === 'en-US'
        ? runtimeConfig.public.localeDomains.en
        : runtimeConfig.public.localeDomains.fr

    if (targetDomain && window.location.host !== targetDomain) {
      window.location.href = `${window.location.protocol}//${targetDomain}${window.location.pathname}${window.location.search}`
    }
  }
}
</script>

<style scoped>
.app-header {
  border-bottom: 1px solid rgba(var(--v-theme-on-surface), 0.08);
}
</style>
