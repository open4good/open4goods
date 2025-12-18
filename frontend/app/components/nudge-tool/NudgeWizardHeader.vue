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
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
    accentCorner: 'top-left',
    cornerSize: 'lg',
    categorySummary: null,
  }
)

const isStacked = computed(() => props.accentCorner === 'top-left')

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

  @media (max-width: 960px) {
    grid-template-columns: 1fr;
    text-align: center;
    justify-items: center;

    &__meta {
      justify-content: center;
      gap: 10px;
    }
  }
}
</style>
