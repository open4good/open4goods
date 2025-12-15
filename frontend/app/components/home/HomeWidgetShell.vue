<script setup lang="ts">
import { computed, useSlots } from 'vue'

const props = withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    icon?: string
    surface?: 'glass' | 'strong'
    dense?: boolean
  }>(),
  {
    title: '',
    subtitle: '',
    icon: '',
    surface: 'glass',
    dense: false,
  },
)

const shellClasses = computed(() => ({
  'home-widget-shell--strong': props.surface === 'strong',
  'home-widget-shell--dense': props.dense,
}))

const hasHeaderContent = computed(
  () => Boolean(props.title || props.subtitle || props.icon || !!useSlots().header || !!useSlots().actions),
)
</script>

<template>
  <div class="home-widget-shell" :class="shellClasses">
    <header v-if="hasHeaderContent" class="home-widget-shell__header">
      <slot name="header">
        <div class="home-widget-shell__header-main">
          <v-avatar v-if="icon" class="home-widget-shell__icon" size="36" color="surface-primary-120" rounded="lg">
            <v-icon :icon="icon" color="accent-primary-highlight" size="24" />
          </v-avatar>
          <div class="home-widget-shell__titles">
            <p v-if="title" class="home-widget-shell__title">{{ title }}</p>
            <p v-if="subtitle" class="home-widget-shell__subtitle">{{ subtitle }}</p>
          </div>
        </div>
        <div v-if="$slots.actions" class="home-widget-shell__actions">
          <slot name="actions" />
        </div>
      </slot>
    </header>

    <div class="home-widget-shell__body">
      <slot />
    </div>

    <footer v-if="$slots.footer" class="home-widget-shell__footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<style scoped lang="scss">
.home-widget-shell {
  --home-widget-radius: 18px;
  --home-widget-radius-emphasis: 26px;
  --home-widget-padding-x: clamp(1.1rem, 3vw, 1.6rem);
  --home-widget-padding-y: clamp(1rem, 2.8vw, 1.5rem);
  --home-widget-gap: clamp(0.75rem, 2vw, 1rem);

  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--home-widget-gap);
  background: rgba(var(--v-theme-surface-glass), 0.95);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.65);
  border-radius: var(--home-widget-radius) var(--home-widget-radius) var(--home-widget-radius-emphasis) var(--home-widget-radius);
  box-shadow: 0 14px 38px rgba(var(--v-theme-shadow-primary-600), 0.14);
  padding: var(--home-widget-padding-y) var(--home-widget-padding-x);
  overflow: hidden;
  backdrop-filter: blur(8px);

  &::after {
    content: '';
    position: absolute;
    width: 120px;
    height: 120px;
    bottom: -46px;
    right: -46px;
    background: radial-gradient(circle at 30% 30%, rgba(var(--v-theme-accent-supporting), 0.35), transparent 70%),
      radial-gradient(circle at 70% 70%, rgba(var(--v-theme-primary), 0.22), transparent 75%);
    border-radius: 60px;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
    filter: blur(0.3px);
    opacity: 0.7;
  }

  &--strong {
    background: rgba(var(--v-theme-surface-glass-strong), 0.96);
  }

  &--dense {
    --home-widget-padding-x: clamp(0.9rem, 2.5vw, 1.25rem);
    --home-widget-padding-y: clamp(0.85rem, 2.2vw, 1.2rem);
    --home-widget-gap: clamp(0.6rem, 1.6vw, 0.9rem);
  }

  &__header {
    display: grid;
    grid-template-columns: 1fr auto;
    align-items: start;
    gap: var(--home-widget-gap);
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
    gap: var(--home-widget-gap);
    position: relative;
    z-index: 1;
  }

  &__footer {
    margin-top: 0.25rem;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--home-widget-gap);
    position: relative;
    z-index: 1;
  }
}
</style>
