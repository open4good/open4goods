<template>
  <header
    class="wizard-header"
    :class="{ 'wizard-header--stacked': isStacked }"
    :style="headerOffsetStyle"
  >
    <div class="wizard-header__titles">
      <div class="wizard-header__title">
        <v-icon
          v-if="titleIcon"
          :icon="titleIcon"
          size="26"
          class="wizard-header__title-icon"
        />
        <h2 class="wizard-header__title-text">{{ title }}</h2>
      </div>
      <p v-if="subtitle" class="wizard-header__subtitle">{{ subtitle }}</p>
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
    titleIcon?: string | null
  }>(),
  {
    subtitle: '',
    accentCorner: 'top-left',
    cornerSize: 'lg',
    titleIcon: null,
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
  grid-template-columns: 1fr;
  gap: 10px;
  align-items: center;
  justify-items: center;
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
    align-items: center;
  }

  &__title {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }

  &__title-text {
    margin: 0;
    font-weight: 800;
    font-size: clamp(1.25rem, 1.8vw, 1.5rem);
    text-align: center;
  }

  &__title-icon {
    color: rgb(var(--v-theme-accent-supporting));
  }

  &__subtitle {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
    text-align: center;
    max-width: 680px;
  }
}
</style>
