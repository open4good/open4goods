<template>
  <v-btn
    :icon="isDark ? 'mdi-weather-sunny' : 'mdi-weather-night'"
    :aria-label="t('theme.toggle')"
    variant="text"
    @click="toggleTheme"
  />
</template>

<script setup lang="ts">
import { useTheme } from 'vuetify'

const { t } = useI18n()
const { activeTheme, setPreference } = useThemePreference()
const vuetifyTheme = useTheme()

const isDark = computed(() => activeTheme.value === 'dark')

watch(activeTheme, (nextTheme) => {
  vuetifyTheme.change(nextTheme)
}, { immediate: true })

function toggleTheme() {
  setPreference(isDark.value ? 'light' : 'dark')
}
</script>
