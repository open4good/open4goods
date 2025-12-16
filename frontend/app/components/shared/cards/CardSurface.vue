<script setup lang="ts">
import { computed, useSlots } from 'vue'

export type CardSurfaceProps = {
  title?: string
  subtitle?: string
  icon?: string
  surface?: 'glass' | 'strong'
  dense?: boolean
  accentCorner?: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right'
  tone?: 'primary' | 'secondary' | 'tertiary'
}

const props = withDefaults(defineProps<CardSurfaceProps>(), {
  title: '',
  subtitle: '',
  icon: '',
  surface: 'glass',
  dense: false,
  accentCorner: 'bottom-right',
  tone: 'primary',
})

const shellClasses = computed(() => ({
  'card-surface--strong': props.surface === 'strong',
  'card-surface--dense': props.dense,
  [`card-surface--tone-${props.tone}`]: true,
  [`card-surface--accent-${props.accentCorner}`]: true,
}))

const hasHeaderContent = computed(() =>
  Boolean(
    props.title ||
    props.subtitle ||
    props.icon ||
    !!useSlots().header ||
    !!useSlots().actions
  )
)
</script>

<template>
  <div class="card-surface" :class="shellClasses">
    <header v-if="hasHeaderContent" class="card-surface__header">
      <slot name="header">
        <div class="card-surface__header-main">
          <v-avatar
            v-if="icon"
            class="card-surface__icon"
            size="36"
            color="surface-primary-120"
            rounded="lg"
          >
            <v-icon :icon="icon" color="accent-primary-highlight" size="24" />
          </v-avatar>
          <div class="card-surface__titles">
            <p v-if="title" class="card-surface__title">{{ title }}</p>
            <p v-if="subtitle" class="card-surface__subtitle">{{ subtitle }}</p>
          </div>
        </div>
        <div v-if="$slots.actions" class="card-surface__actions">
          <slot name="actions" />
        </div>
      </slot>
    </header>

    <div class="card-surface__body">
      <slot />
    </div>

    <footer v-if="$slots.footer" class="card-surface__footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<style scoped lang="scss">
.card-surface {
  --card-radius: 18px;
  --card-radius-emphasis: 26px;
  --card-padding-x: clamp(1.1rem, 3vw, 1.6rem);
  --card-padding-y: clamp(1rem, 2.8vw, 1.5rem);
  --card-gap: clamp(0.75rem, 2vw, 1rem);
  --card-border-color: rgba(var(--v-theme-border-primary-strong), 0.65);
  --card-highlight-main: rgba(var(--v-theme-accent-supporting), 0.35);
  --card-highlight-alt: rgba(var(--v-theme-primary), 0.22);

  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--card-gap);
  background: rgba(var(--v-theme-surface-glass), 0.95);
  border: 1px solid var(--card-border-color);
  border-radius: var(--card-radius) var(--card-radius)
    var(--card-radius-emphasis) var(--card-radius);
  box-shadow: 0 14px 38px rgba(var(--v-theme-shadow-primary-600), 0.14);
  padding: var(--card-padding-y) var(--card-padding-x);
  overflow: hidden;
  backdrop-filter: blur(8px);

  &::after {
    content: '';
    position: absolute;
    width: 120px;
    height: 120px;
    bottom: -46px;
    right: -46px;
    background:
      radial-gradient(
        circle at 30% 30%,
        var(--card-highlight-main),
        transparent 70%
      ),
      radial-gradient(
        circle at 70% 70%,
        var(--card-highlight-alt),
        transparent 75%
      );
    border-radius: 60px;
    border: 1px solid var(--card-border-color);
    filter: blur(0.3px);
    opacity: 0.7;
  }

  &--strong {
    background: rgba(var(--v-theme-surface-glass-strong), 0.96);
  }

  &--dense {
    --card-padding-x: clamp(0.9rem, 2.5vw, 1.25rem);
    --card-padding-y: clamp(0.85rem, 2.2vw, 1.2rem);
    --card-gap: clamp(0.6rem, 1.6vw, 0.9rem);
  }

  &--tone-primary {
    --card-border-color: rgba(var(--v-theme-accent-supporting), 0.55);
    --card-highlight-main: rgba(var(--v-theme-accent-supporting), 0.4);
    --card-highlight-alt: rgba(var(--v-theme-primary), 0.28);
  }

  &--tone-secondary {
    --card-border-color: rgba(var(--v-theme-border-primary-strong), 0.7);
    --card-highlight-main: rgba(var(--v-theme-hero-gradient-end), 0.32);
    --card-highlight-alt: rgba(var(--v-theme-hero-gradient-start), 0.24);
  }

  &--tone-tertiary {
    --card-border-color: rgba(var(--v-theme-surface-primary-080), 0.9);
    --card-highlight-main: rgba(var(--v-theme-surface-primary-120), 0.6);
    --card-highlight-alt: rgba(var(--v-theme-surface-primary-100), 0.45);
  }

  &--accent-top-left {
    border-radius: var(--card-radius-emphasis) var(--card-radius)
      var(--card-radius) var(--card-radius);
  }

  &--accent-top-right {
    border-radius: var(--card-radius) var(--card-radius-emphasis)
      var(--card-radius) var(--card-radius);
  }

  &--accent-bottom-left {
    border-radius: var(--card-radius) var(--card-radius) var(--card-radius)
      var(--card-radius-emphasis);
  }

  &--accent-bottom-right {
    border-radius: var(--card-radius) var(--card-radius)
      var(--card-radius-emphasis) var(--card-radius);
  }

  &__header {
    display: grid;
    grid-template-columns: 1fr auto;
    align-items: start;
    gap: var(--card-gap);
  }

  &__header-main {
    display: flex;
    align-items: flex-start;
    gap: 0.65rem;
  }

  &__titles {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-weight: 700;
    font-size: 1.1rem;
    color: rgb(var(--v-theme-text-neutral-strong));
    line-height: 1.2;
  }

  &__subtitle {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
    line-height: 1.35;
    font-weight: 500;
  }

  &__icon {
    box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.55);
  }

  &__actions {
    display: flex;
    gap: 0.5rem;
    align-items: center;
    justify-content: flex-end;
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: var(--card-gap);
    position: relative;
    z-index: 1;
  }

  &__footer {
    margin-top: 0.25rem;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--card-gap);
    position: relative;
    z-index: 1;
  }
}
</style>
