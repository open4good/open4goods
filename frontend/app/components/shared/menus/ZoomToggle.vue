<template>
  <v-tooltip location="bottom">
    <template #activator="{ props: tooltipProps }">
      <v-btn
        v-bind="tooltipProps"
        class="zoom-toggle ms-2"
        :color="isZoomed ? 'primary' : 'default'"
        :variant="isZoomed ? 'flat' : 'text'"
        icon
        :density="density"
        :size="size"
        :data-testid="testId"
        :aria-label="t('siteIdentity.menu.zoom.label')"
        :aria-pressed="isZoomed"
        @click="toggleZoom"
      >
        <v-icon
          :icon="isZoomed ? 'mdi-face-woman' : 'mdi-face-woman-outline'"
        />
      </v-btn>
    </template>
    {{
      isZoomed
        ? t('siteIdentity.menu.zoom.reset')
        : t('siteIdentity.menu.zoom.activate')
    }}
  </v-tooltip>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'
import { useAccessibilityStore } from '~/stores/useAccessibilityStore'

withDefaults(
  defineProps<{
    density?: 'default' | 'comfortable' | 'compact'
    size?: 'x-small' | 'small' | 'default'
    testId?: string
  }>(),
  {
    density: 'comfortable',
    size: 'small',
    testId: 'zoom-toggle',
  }
)

const { t } = useI18n()
const accessibilityStore = useAccessibilityStore()
const { isZoomed } = storeToRefs(accessibilityStore)
const { toggleZoom } = accessibilityStore
</script>

<style scoped lang="sass">
.zoom-toggle
  color: rgb(var(--v-theme-text-neutral-strong))

  &.v-btn--active
    color: rgb(var(--v-theme-accent-supporting))

  &:focus-visible
    outline: 2px solid rgba(var(--v-theme-accent-supporting), 0.6)
    outline-offset: 3px
</style>
