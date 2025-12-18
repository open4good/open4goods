<template>
  <v-btn
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
      :icon="
        isZoomed ? 'mdi-face-woman' : 'mdi-face-woman-outline'
      "
    />
    <v-tooltip activator="parent" location="bottom">
      {{
        isZoomed
          ? t('siteIdentity.menu.zoom.reset')
          : t('siteIdentity.menu.zoom.activate')
      }}
    </v-tooltip>
  </v-btn>
</template>

<script setup lang="ts">
import { useStorage } from '@vueuse/core'
import { watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'

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

const isZoomed = useStorage('is-zoomed', false)

const toggleZoom = () => {
  isZoomed.value = !isZoomed.value
}

const applyZoom = (zoomed: boolean) => {
  if (typeof document === 'undefined') {
    return
  }

  const rootElement = document.documentElement

  rootElement.style.fontSize = zoomed ? '120%' : ''
  rootElement.classList.toggle('accessibility-layout', zoomed)
}

watch(isZoomed, val => {
  applyZoom(val)
})

onMounted(() => {
  applyZoom(isZoomed.value)
})
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
