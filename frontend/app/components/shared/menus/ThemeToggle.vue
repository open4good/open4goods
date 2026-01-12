<template>
  <v-btn-toggle
    v-model="selectedTheme"
    class="theme-toggle"
    :class="{ 'theme-toggle--labeled': showLabels }"
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
          <span class="theme-toggle__content">
            <v-icon icon="mdi-white-balance-sunny" />
            <span v-if="showLabels" class="theme-toggle__label">
              {{ lightLabel }}
            </span>
          </span>
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
          <span class="theme-toggle__content">
            <v-icon icon="mdi-weather-night" />
            <span v-if="showLabels" class="theme-toggle__label">
              {{ darkLabel }}
            </span>
          </span>
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

import {
  THEME_PREFERENCE_KEY,
  resolveThemeName,
  type ThemeName,
} from '~~/shared/constants/theme'

const props = withDefaults(
  defineProps<{
    density?: 'default' | 'comfortable' | 'compact'
    size?: 'x-small' | 'small' | 'default'
    testId?: string
    showLabels?: boolean
  }>(),
  {
    density: 'comfortable',
    size: 'small',
    testId: 'theme-toggle',
    showLabels: false,
  }
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
  resolveThemeName(themeCookie.value, preferredDark.value ? 'dark' : 'light')
)

const applyTheme = (value: ThemeName) => {
  if (theme.global.name.value !== value) {
    theme.global.name.value = value
  }

  if (storedPreference.value !== value) {
    storedPreference.value = value
  }

  const isClient = typeof window !== 'undefined'
  const shouldPersistCookie =
    isClient || value === 'dark' || themeCookie.value != null

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
  { immediate: true }
)

const selectedTheme = computed<ThemeName>({
  get: () => resolveThemeName(theme.global.name.value),
  set: value => applyTheme(value),
})

const lightTooltip = computed(() => t('siteIdentity.menu.theme.lightTooltip'))
const darkTooltip = computed(() => t('siteIdentity.menu.theme.darkTooltip'))
const lightAriaLabel = computed(() =>
  t('siteIdentity.menu.theme.lightAriaLabel')
)
const darkAriaLabel = computed(() => t('siteIdentity.menu.theme.darkAriaLabel'))
const lightLabel = computed(() => t('siteIdentity.menu.theme.lightTooltip'))
const darkLabel = computed(() => t('siteIdentity.menu.theme.darkTooltip'))

const density = toRef(props, 'density')
const size = toRef(props, 'size')
const testId = toRef(props, 'testId')
const showLabels = toRef(props, 'showLabels')
</script>

<style scoped lang="sass">
.theme-toggle
  background-color: rgba(var(--v-theme-surface-muted), 0.6)
  :deep(.v-btn)
    color: rgb(var(--v-theme-text-neutral-strong))
  :deep(.v-btn.v-btn--active)
    color: rgb(var(--v-theme-accent-supporting))

.theme-toggle__content
  display: flex
  flex-direction: column
  align-items: center
  gap: 0.35rem

.theme-toggle__label
  font-size: 0.72rem
  font-weight: 600
  line-height: 1.1
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.theme-toggle--labeled
  :deep(.v-btn)
    padding-inline: 0.85rem
</style>
