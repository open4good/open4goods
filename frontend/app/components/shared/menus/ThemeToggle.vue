<template>
  <v-btn-toggle
    v-model="selectedTheme"
    class="theme-toggle"
    :density="density"
    color="accent"
    mandatory
    rounded="pill"
    :data-testid="testId"
  >

    <v-tooltip :text="lightTooltip" location="bottom">
      <template #activator="{ props: tooltipProps }">
        <v-btn
          v-bind="tooltipProps"
          :value="'light'"
          :aria-label="lightAriaLabel"
          :data-testid="`${testId}-light`"
          :size="size"
          icon
          variant="plain"
        >
          <v-icon icon="mdi-white-balance-sunny" />
        </v-btn>
      </template>
    </v-tooltip>

    <v-tooltip :text="darkTooltip" location="bottom">
      <template #activator="{ props: tooltipProps }">
        <v-btn
          v-bind="tooltipProps"
          :value="'dark'"
          :aria-label="darkAriaLabel"
          :data-testid="`${testId}-dark`"
          :size="size"
          icon
          variant="plain"
        >
          <v-icon icon="mdi-weather-night" />
        </v-btn>
      </template>
    </v-tooltip>

    <v-tooltip :text="nudgerTooltip" location="bottom">
      <template #activator="{ props: tooltipProps }">
        <v-btn
          v-bind="tooltipProps"
          :value="'nudger'"
          :aria-label="nudgerAriaLabel"
          :data-testid="`${testId}-nudger`"
          :size="size"
          icon
          variant="plain"
        >
          <v-icon icon="mdi-palette-swatch-variant" />
        </v-btn>
      </template>
    </v-tooltip>
  </v-btn-toggle>
</template>

<script setup lang="ts">
import { usePreferredDark, useStorage } from '@vueuse/core'
import { computed, toRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTheme } from 'vuetify'

import { THEME_PREFERENCE_KEY, resolveThemeName, type ThemeName } from '~~/shared/constants/theme'

const props = withDefaults(
  defineProps<{
    density?: 'default' | 'comfortable' | 'compact'
    size?: 'x-small' | 'small' | 'default'
    testId?: string
  }>(),
  {
    density: 'comfortable',
    size: 'small',
    testId: 'theme-toggle',
  },
)

const { t } = useI18n()
const theme = useTheme()
const preferredDark = usePreferredDark()
const themeCookie = useCookie<string | null>(THEME_PREFERENCE_KEY, {
  sameSite: 'lax',
  path: '/',
  watch: false,
})
const storedPreference = useStorage<ThemeName>(
  THEME_PREFERENCE_KEY,
  resolveThemeName(themeCookie.value, preferredDark.value ? 'dark' : 'light'),
)

const applyTheme = (value: ThemeName) => {
  if (theme.global.name.value !== value) {
    theme.global.name.value = value
  }

  if (storedPreference.value !== value) {
    storedPreference.value = value
  }

  const isClient = typeof window !== 'undefined'
  const shouldPersistCookie = isClient || value === 'dark' || themeCookie.value != null

  if (shouldPersistCookie && themeCookie.value !== value) {
    themeCookie.value = value
  }
}

watch(
  [storedPreference, preferredDark],
  ([stored, prefersDark]) => {
    const nextTheme = resolveThemeName(stored, prefersDark ? 'dark' : 'light')
    applyTheme(nextTheme)
  },
  { immediate: true },
)

const selectedTheme = computed<ThemeName>({
  get: () => resolveThemeName(theme.global.name.value),
  set: (value) => applyTheme(value),
})

const lightTooltip = computed(() => t('siteIdentity.menu.theme.lightTooltip'))
const darkTooltip = computed(() => t('siteIdentity.menu.theme.darkTooltip'))
const lightAriaLabel = computed(() => t('siteIdentity.menu.theme.lightAriaLabel'))
const darkAriaLabel = computed(() => t('siteIdentity.menu.theme.darkAriaLabel'))
const nudgerTooltip = computed(() => t('siteIdentity.menu.theme.nudgerTooltip'))
const nudgerAriaLabel = computed(() => t('siteIdentity.menu.theme.nudgerAriaLabel'))

const density = toRef(props, 'density')
const size = toRef(props, 'size')
const testId = toRef(props, 'testId')
</script>

<style scoped lang="sass">
.theme-toggle
  background-color: rgba(var(--v-theme-surface-muted), 0.6)
  :deep(.v-btn)
    color: rgb(var(--v-theme-text-neutral-strong))
  :deep(.v-btn.v-btn--active)
    color: rgb(var(--v-theme-accent-supporting))
</style>
