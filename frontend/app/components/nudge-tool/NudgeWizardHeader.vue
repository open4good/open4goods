<template>
  <header
    class="wizard-header"
    :class="{ 'wizard-header--stacked': isStacked }"
    :style="headerOffsetStyle"
  >
    <div class="wizard-header__titles">
      <h2 class="wizard-header__title">{{ title }}</h2>
      <p v-if="subtitle" class="wizard-header__subtitle">{{ subtitle }}</p>
    </div>

    <div class="wizard-header__meta">
      <div v-if="categorySummary" class="wizard-header__category" aria-live="polite">
        <v-avatar size="38" class="wizard-header__category-avatar" color="primary">
          <v-icon :icon="categorySummary.icon" size="24" color="white" />
        </v-avatar>
        <div class="wizard-header__category-text">
          <span class="wizard-header__category-label">{{ categorySummary.label }}</span>
          <span v-if="categorySummary.matches != null" class="wizard-header__category-meta">
            {{ categorySummary.matches }}
          </span>
        </div>
      </div>

      <div
        class="wizard-header__actions"
        :class="{ 'wizard-header__actions--mobile': isMobile }"
      >
        <v-btn-group
          class="wizard-header__action-group"
          :class="{ 'wizard-header__action-group--mobile': isMobile }"
          density="comfortable"
        >
          <v-btn
            v-if="hasPrevious"
            color="primary"
            variant="outlined"
            rounded="pill"
            elevation="0"
            prepend-icon="mdi-chevron-left"
            class="wizard-header__action-btn wizard-header__action-btn--previous"
            @click="emit('previous')"
          >
            {{ previousLabel }}
          </v-btn>

          <v-btn
            v-if="hasNext"
            color="primary"
            variant="tonal"
            rounded="pill"
            elevation="0"
            :disabled="nextDisabled"
            append-icon="mdi-chevron-right"
            class="wizard-header__action-btn wizard-header__action-btn--next"
            @click="emit('next')"
          >
            {{ nextLabel }}
          </v-btn>
        </v-btn-group>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useDisplay } from 'vuetify'
import type {
  AccentCorner,
  CornerSize,
} from '~/components/shared/cards/RoundedCornerCard.vue'

const cornerSizeOffsets: Record<CornerSize, string> = {
  sm: '46px',
  md: '58px',
  lg: '72px',
  xl: '120px',
}

const props = withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    hasPrevious?: boolean
    hasNext?: boolean
    nextDisabled?: boolean
    previousLabel: string
    nextLabel: string
    accentCorner?: AccentCorner
    cornerSize?: CornerSize
    categorySummary?: {
      label: string
      icon?: string
      matches?: number
    } | null
  }>(),
  {
    subtitle: '',
    hasPrevious: false,
    hasNext: false,
    nextDisabled: false,
    accentCorner: 'top-left',
    cornerSize: 'lg',
    categorySummary: null,
  }
)

const emit = defineEmits<{
  (e: 'previous' | 'next'): void
}>()

const isStacked = computed(() => props.accentCorner === 'top-left')
const display = useDisplay()
const isMobile = computed(() => {
  const breakpoint = display && 'mdAndDown' in display ? display.mdAndDown : undefined
  return typeof breakpoint?.value === 'boolean' ? breakpoint.value : false
})

const headerOffsetStyle = computed(() => {
  if (props.accentCorner !== 'top-left') {
    return undefined
  }

  return {
    paddingLeft: cornerSizeOffsets[props.cornerSize],
  }
})
</script>

<style scoped lang="scss">
.wizard-header {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;

  &--stacked {
    @media (max-width: 960px) {
      grid-template-columns: 1fr;
      padding-left: 0 !important;
    }
  }

  &__titles {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__title {
    margin: 0;
    font-weight: 800;
    font-size: clamp(1.25rem, 1.8vw, 1.5rem);
    text-align: center;
  }

  &__subtitle {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
    text-align: center;
  }

  &__meta {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  &__category {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 10px;
    border-radius: 999px;
    background: rgba(var(--v-theme-surface-primary-080), 0.85);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5);
  }

  &__category-avatar {
    flex-shrink: 0;
  }

  &__category-text {
    display: flex;
    flex-direction: column;
    line-height: 1.15;
  }

  &__category-label {
    font-weight: 700;
  }

  &__category-meta {
    font-size: 0.85rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }

  &__action-group {
    display: inline-flex;
    gap: 6px;
    padding: 6px 8px;
    border-radius: 999px;
    background-color: rgba(var(--v-theme-surface-primary-080), 0.55);
    border: 1px solid rgb(var(--v-theme-border-primary-strong));
    transition: background-color 160ms ease, border-color 160ms ease;
  }

  &__action-group--mobile {
    width: 100%;
    max-width: 420px;
    flex-direction: column;
    justify-content: center;
    align-items: stretch;
    padding: 10px;
    gap: 10px;
  }

  &__action-btn {
    font-weight: 700;

    :deep(.v-btn) {
      border-width: 1px;
      transition:
        background-color 160ms ease,
        color 160ms ease,
        border-color 160ms ease;
    }

    :deep(.v-btn:hover),
    :deep(.v-btn:focus-visible) {
      background-color: rgba(var(--v-theme-surface-primary-100), 0.75);
      color: rgb(var(--v-theme-text-neutral-strong));
      box-shadow: none;
    }

    :deep(.v-btn:focus-visible) {
      outline: 2px solid rgb(var(--v-theme-accent-primary-highlight));
      outline-offset: 2px;
    }

    :deep(.v-btn--disabled) {
      color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
      border-color: rgba(var(--v-theme-border-primary-strong), 0.5);
      background-color: rgba(var(--v-theme-surface-primary-080), 0.55);
      box-shadow: none;
    }
  }

  &__actions--mobile {
    justify-content: center;
  }

  &__action-btn--next :deep(.v-btn) {
    color: rgb(var(--v-theme-text-on-accent));
  }

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
    text-align: center;
    justify-items: center;

    &__meta {
      justify-content: center;
      gap: 10px;
    }

    &__actions {
      width: 100%;
      order: 2;
    }

    &__action-group--mobile :deep(.v-btn) {
      width: 100%;
    }
  }
}
</style>
