<template>
  <v-sheet
    class="pwa-action-bar"
    elevation="12"
    border
    color="surface-default"
    role="navigation"
    :aria-label="t('pwa.landing.actionBar.ariaLabel')"
  >
    <v-container class="py-2">
      <div class="pwa-action-bar__grid">
        <v-btn
          v-for="action in actions"
          :key="action.key"
          :data-testid="`pwa-action-${action.key}`"
          class="pwa-action-bar__btn"
          variant="flat"
          color="primary"
          size="large"
          block
          :prepend-icon="action.icon"
          @click="emit(action.event)"
        >
          <span class="pwa-action-bar__label">{{ action.label }}</span>
        </v-btn>
      </div>
    </v-container>
  </v-sheet>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const emit = defineEmits<{
  (event: 'scan' | 'wizard' | 'search'): void
}>()

const { t } = useI18n()

const actions = computed(() => [
  {
    key: 'scan',
    icon: 'mdi-barcode-scan',
    label: t('pwa.landing.actions.scan.title'),
    event: 'scan' as const,
  },
  {
    key: 'wizard',
    icon: 'mdi-sparkles',
    label: t('pwa.landing.actions.wizard.title'),
    event: 'wizard' as const,
  },
  {
    key: 'search',
    icon: 'mdi-magnify',
    label: t('pwa.landing.actions.search.title'),
    event: 'search' as const,
  },
])
</script>

<style scoped lang="sass">
.pwa-action-bar
  position: sticky
  bottom: 0
  z-index: 12
  padding-bottom: env(safe-area-inset-bottom, 0px)

.pwa-action-bar__grid
  display: grid
  grid-template-columns: repeat(3, 1fr)
  gap: 0.5rem

.pwa-action-bar__btn
  text-transform: none
  border-radius: 12px
  font-weight: 700
  justify-content: flex-start
  padding-inline: 1rem

  :deep(.v-btn__content)
    gap: 0.35rem

.pwa-action-bar__label
  white-space: nowrap
  overflow: hidden
  text-overflow: ellipsis

@media (max-width: 340px)
  .pwa-action-bar__grid
    grid-template-columns: 1fr

  .pwa-action-bar__btn
    justify-content: center

  @media (max-width: 380px)
    .pwa-action-bar__label
      display: none

    .pwa-action-bar__btn
      justify-content: center
      padding-inline: 0

    .pwa-action-bar__grid
      grid-template-columns: repeat(3, 1fr)
</style>
